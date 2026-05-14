use anyhow::{Context, Result, anyhow};
use chrono::{NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use serde_json::{Value, json};
use sha2::{Digest, Sha256};
use uuid::Uuid;

use super::dto::{AnalysisKind, GenerateReportRequest, ReportType};
use crate::state::AppState;

const TOKEN_PROGRAM_ID: &str = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
const ASSOC_TOKEN_PROGRAM_ID: &str = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL";
const MEMO_PROGRAM_ID: &str = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr";
const MAX_TX_AGE_SECONDS: i64 = 3600;

#[derive(Debug, Deserialize)]
struct AnalysisUsageRow {
    free_used_count: i32,
}

#[derive(Debug, Deserialize)]
struct PaymentHeaderPayload {
    signature: String,
    #[serde(rename = "buyerAddress")]
    buyer_address: String,
    network: String,
}

#[derive(Debug, Clone)]
pub struct AnalysisCharge {
    pub analysis_kind: AnalysisKind,
    pub report_type: ReportType,
    pub period_start: NaiveDate,
    pub amount_micro: u64,
}

#[derive(Debug, Serialize)]
pub struct Solana402Body {
    #[serde(rename = "x402Version")]
    pub x402_version: u8,
    pub error: &'static str,
    pub accepts: Vec<SolanaPaymentRequirement>,
}

#[derive(Debug, Serialize)]
pub struct SolanaPaymentRequirement {
    pub scheme: &'static str,
    pub network: String,
    #[serde(rename = "maxAmountRequired")]
    pub max_amount_required: String,
    pub resource: String,
    pub description: String,
    #[serde(rename = "mimeType")]
    pub mime_type: &'static str,
    #[serde(rename = "payTo")]
    pub pay_to: String,
    #[serde(rename = "maxTimeoutSeconds")]
    pub max_timeout_seconds: i64,
    pub asset: String,
    pub extra: Value,
}

pub enum AnalysisAccess {
    Free,
    Paid { tx_signature: String },
    PaymentRequired(Solana402Body),
}

pub async fn authorize_analysis(
    state: &AppState,
    user_id: Uuid,
    req: &GenerateReportRequest,
    payment_header: Option<&str>,
    resource: &str,
) -> Result<AnalysisAccess> {
    let charge = charge_for_request(state, req)?;
    let usage = get_or_create_usage(state, user_id, &charge).await?;
    let free_limit = free_limit_for(state, req.report_type);

    if usage.free_used_count < free_limit {
        return Ok(AnalysisAccess::Free);
    }

    let Some(header) = payment_header else {
        return Ok(AnalysisAccess::PaymentRequired(build_402(
            state, user_id, req, &charge, resource,
        )?));
    };

    let payment = parse_payment_header(header)?;
    let request_hash = request_hash(req)?;
    let payment_memo = payment_memo(user_id, &request_hash);
    verify_payment_payload(state, &payment, charge.amount_micro, &payment_memo).await?;
    record_payment(
        state,
        user_id,
        &charge,
        &payment,
        resource,
        &request_hash,
        &payment_memo,
    )
    .await?;

    Ok(AnalysisAccess::Paid {
        tx_signature: payment.signature,
    })
}

pub async fn record_usage(
    state: &AppState,
    user_id: Uuid,
    req: &GenerateReportRequest,
    paid: bool,
) -> Result<()> {
    let charge = charge_for_request(state, req)?;
    let row = get_or_create_usage(state, user_id, &charge).await?;
    let (column, next_value) = if paid {
        (
            "paid_used_count",
            row_paid_count(state, user_id, &charge).await? + 1,
        )
    } else {
        ("free_used_count", row.free_used_count + 1)
    };

    let url = format!(
        "{}/rest/v1/analysis_usage?user_id=eq.{}&analysis_kind=eq.{}&report_type=eq.{}&period_start=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        charge.analysis_kind.as_str(),
        charge.report_type.as_str(),
        charge.period_start
    );

    let mut body = serde_json::Map::new();
    body.insert(column.to_string(), json!(next_value));

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .json(&body)
        .send()
        .await
        .context("analysis_usage PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("analysis_usage PATCH 실패: {}", body));
    }

    Ok(())
}

pub async fn mark_payment_consumed(state: &AppState, tx_signature: &str) -> Result<()> {
    let url = format!(
        "{}/rest/v1/analysis_payments?tx_signature=eq.{}&consumed_at=is.null",
        state.config.supabase_url.trim_end_matches('/'),
        tx_signature
    );
    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .json(&json!({ "consumed_at": Utc::now() }))
        .send()
        .await
        .context("analysis_payments consumed PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("analysis_payments consumed PATCH 실패: {}", body));
    }

    Ok(())
}

fn charge_for_request(state: &AppState, req: &GenerateReportRequest) -> Result<AnalysisCharge> {
    let period_start =
        NaiveDate::parse_from_str(&req.start_date, "%Y-%m-%d").context("start_date 형식 오류")?;
    let amount_micro = match req.report_type {
        ReportType::Weekly => state.config.analysis_weekly_price_micro,
        ReportType::Monthly => state.config.analysis_monthly_price_micro,
    };

    Ok(AnalysisCharge {
        analysis_kind: req.analysis_kind,
        report_type: req.report_type,
        period_start,
        amount_micro,
    })
}

fn free_limit_for(state: &AppState, report_type: ReportType) -> i32 {
    match report_type {
        ReportType::Weekly => state.config.analysis_weekly_free_limit,
        ReportType::Monthly => state.config.analysis_monthly_free_limit,
    }
}

async fn get_or_create_usage(
    state: &AppState,
    user_id: Uuid,
    charge: &AnalysisCharge,
) -> Result<AnalysisUsageRow> {
    if let Some(row) = get_usage(state, user_id, charge).await? {
        return Ok(row);
    }

    #[derive(Serialize)]
    struct InsertUsage<'a> {
        user_id: Uuid,
        analysis_kind: &'a str,
        report_type: &'a str,
        period_start: NaiveDate,
    }

    let url = format!(
        "{}/rest/v1/analysis_usage",
        state.config.supabase_url.trim_end_matches('/')
    );
    let res = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .json(&InsertUsage {
            user_id,
            analysis_kind: charge.analysis_kind.as_str(),
            report_type: charge.report_type.as_str(),
            period_start: charge.period_start,
        })
        .send()
        .await
        .context("analysis_usage INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        if !body.contains("duplicate") && !body.contains("23505") {
            return Err(anyhow!("analysis_usage INSERT 실패: {}", body));
        }
    }

    get_usage(state, user_id, charge)
        .await?
        .ok_or_else(|| anyhow!("analysis_usage row 생성 후 조회 실패"))
}

async fn get_usage(
    state: &AppState,
    user_id: Uuid,
    charge: &AnalysisCharge,
) -> Result<Option<AnalysisUsageRow>> {
    let url = format!(
        "{}/rest/v1/analysis_usage?user_id=eq.{}&analysis_kind=eq.{}&report_type=eq.{}&period_start=eq.{}&select=free_used_count&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        charge.analysis_kind.as_str(),
        charge.report_type.as_str(),
        charge.period_start
    );
    let rows: Vec<AnalysisUsageRow> = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("analysis_usage SELECT 요청 실패")?
        .json()
        .await
        .context("analysis_usage SELECT 응답 파싱 실패")?;

    Ok(rows.into_iter().next())
}

async fn row_paid_count(state: &AppState, user_id: Uuid, charge: &AnalysisCharge) -> Result<i32> {
    #[derive(Deserialize)]
    struct Row {
        paid_used_count: i32,
    }

    let url = format!(
        "{}/rest/v1/analysis_usage?user_id=eq.{}&analysis_kind=eq.{}&report_type=eq.{}&period_start=eq.{}&select=paid_used_count&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        charge.analysis_kind.as_str(),
        charge.report_type.as_str(),
        charge.period_start
    );
    let rows: Vec<Row> = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("analysis_usage paid SELECT 요청 실패")?
        .json()
        .await
        .context("analysis_usage paid SELECT 응답 파싱 실패")?;

    Ok(rows
        .into_iter()
        .next()
        .map(|r| r.paid_used_count)
        .unwrap_or(0))
}

fn build_402(
    state: &AppState,
    user_id: Uuid,
    req: &GenerateReportRequest,
    charge: &AnalysisCharge,
    resource: &str,
) -> Result<Solana402Body> {
    if state.config.solana_platform_wallet.trim().is_empty() {
        return Err(anyhow!("SOLANA_PLATFORM_WALLET 환경변수 없음"));
    }

    let request_hash = request_hash(req)?;
    let payment_memo = payment_memo(user_id, &request_hash);

    Ok(Solana402Body {
        x402_version: 1,
        error: "Payment required",
        accepts: vec![SolanaPaymentRequirement {
            scheme: "exact",
            network: state.config.solana_x402_network.clone(),
            max_amount_required: charge.amount_micro.to_string(),
            resource: resource.to_string(),
            description: format!(
                "Spentopia {} {} AI analysis",
                charge.report_type.as_str(),
                charge.analysis_kind.as_str()
            ),
            mime_type: "application/json",
            pay_to: state.config.solana_platform_wallet.clone(),
            max_timeout_seconds: MAX_TX_AGE_SECONDS,
            asset: state.config.solana_usdc_mint.clone(),
            extra: json!({
                "analysisKind": charge.analysis_kind.as_str(),
                "reportType": charge.report_type.as_str(),
                "periodStart": charge.period_start,
                "requestHash": request_hash,
                "paymentMemo": payment_memo,
            }),
        }],
    })
}

fn parse_payment_header(header: &str) -> Result<PaymentHeaderPayload> {
    let decoded = base64_decode(header)?;
    let text = String::from_utf8(decoded).context("X-PAYMENT UTF-8 디코딩 실패")?;
    serde_json::from_str::<PaymentHeaderPayload>(&text).context("X-PAYMENT JSON 파싱 실패")
}

async fn verify_payment_payload(
    state: &AppState,
    payment: &PaymentHeaderPayload,
    min_amount_micro: u64,
    expected_memo: &str,
) -> Result<()> {
    if payment.network != state.config.solana_x402_network {
        return Err(anyhow!(
            "결제 네트워크 불일치: expected={}, got={}",
            state.config.solana_x402_network,
            payment.network
        ));
    }

    crate::clients::solana_client::check_signature_confirmed(
        &state.config.solana_rpc_url,
        &state.http_client,
        &payment.signature,
    )
    .await?;

    let tx = get_parsed_transaction(state, &payment.signature).await?;
    if !tx["meta"]["err"].is_null() {
        return Err(anyhow!("실패한 Solana 트랜잭션입니다"));
    }

    let block_time = tx["blockTime"]
        .as_i64()
        .ok_or_else(|| anyhow!("트랜잭션 blockTime 없음"))?;
    let now = Utc::now().timestamp();
    if now - block_time > MAX_TX_AGE_SECONDS {
        return Err(anyhow!("결제 트랜잭션이 너무 오래됐습니다"));
    }

    let recipient_ata = derive_associated_token_address(
        &state.config.solana_platform_wallet,
        &state.config.solana_usdc_mint,
    )?;
    let amount = find_usdc_transfer_amount(&tx, &recipient_ata, &payment.buyer_address)?;
    verify_payment_memo(&tx, expected_memo)?;

    if amount < min_amount_micro {
        return Err(anyhow!(
            "결제 금액 부족: got={}, expected={}",
            amount,
            min_amount_micro
        ));
    }

    Ok(())
}

async fn get_parsed_transaction(state: &AppState, signature: &str) -> Result<Value> {
    for attempt in 0..6 {
        let res: Value = state
            .http_client
            .post(&state.config.solana_rpc_url)
            .json(&json!({
                "jsonrpc": "2.0",
                "id": 1,
                "method": "getTransaction",
                "params": [
                    signature,
                    {
                        "encoding": "jsonParsed",
                        "commitment": "confirmed",
                        "maxSupportedTransactionVersion": 0
                    }
                ]
            }))
            .send()
            .await
            .context("getTransaction 요청 실패")?
            .json()
            .await
            .context("getTransaction 응답 파싱 실패")?;

        if let Some(err) = res.get("error") {
            return Err(anyhow!("getTransaction 실패: {}", err));
        }

        if !res["result"].is_null() {
            return Ok(res["result"].clone());
        }

        if attempt < 5 {
            tokio::time::sleep(std::time::Duration::from_millis(800)).await;
        }
    }

    Err(anyhow!("트랜잭션을 찾을 수 없습니다: {}", signature))
}

fn find_usdc_transfer_amount(tx: &Value, recipient_ata: &str, buyer_wallet: &str) -> Result<u64> {
    let mut total = 0_u64;

    let mut inspect_ix = |ix: &Value| {
        let parsed = &ix["parsed"];
        let ix_type = parsed["type"].as_str().unwrap_or("");
        if ix_type != "transfer" && ix_type != "transferChecked" {
            return;
        }

        let info = &parsed["info"];
        let destination = info["destination"].as_str().unwrap_or("");
        let authority = info["authority"].as_str().unwrap_or("");
        if destination != recipient_ata || authority != buyer_wallet {
            return;
        }

        let raw_amount = info["tokenAmount"]["amount"]
            .as_str()
            .or_else(|| info["amount"].as_str())
            .unwrap_or("0");
        if let Ok(amount) = raw_amount.parse::<u64>() {
            total = total.saturating_add(amount);
        }
    };

    if let Some(instructions) = tx["transaction"]["message"]["instructions"].as_array() {
        for ix in instructions {
            inspect_ix(ix);
        }
    }

    if let Some(inner_groups) = tx["meta"]["innerInstructions"].as_array() {
        for group in inner_groups {
            if let Some(instructions) = group["instructions"].as_array() {
                for ix in instructions {
                    inspect_ix(ix);
                }
            }
        }
    }

    if total == 0 {
        return Err(anyhow!("플랫폼 USDC ATA로 전송된 결제를 찾을 수 없습니다"));
    }

    Ok(total)
}

fn verify_payment_memo(tx: &Value, expected_memo: &str) -> Result<()> {
    let mut found = false;
    let mut inspect_ix = |ix: &Value| {
        if ix["programId"].as_str() != Some(MEMO_PROGRAM_ID)
            && ix["program"].as_str() != Some("spl-memo")
        {
            return;
        }

        let parsed_memo = ix["parsed"].as_str().unwrap_or("");
        if parsed_memo == expected_memo {
            found = true;
        }
    };

    if let Some(instructions) = tx["transaction"]["message"]["instructions"].as_array() {
        for ix in instructions {
            inspect_ix(ix);
        }
    }

    if let Some(inner_groups) = tx["meta"]["innerInstructions"].as_array() {
        for group in inner_groups {
            if let Some(instructions) = group["instructions"].as_array() {
                for ix in instructions {
                    inspect_ix(ix);
                }
            }
        }
    }

    if !found {
        return Err(anyhow!("결제 memo가 현재 분석 요청과 일치하지 않습니다"));
    }

    Ok(())
}

async fn record_payment(
    state: &AppState,
    user_id: Uuid,
    charge: &AnalysisCharge,
    payment: &PaymentHeaderPayload,
    resource: &str,
    request_hash: &str,
    payment_memo: &str,
) -> Result<()> {
    #[derive(Serialize)]
    struct InsertPayment<'a> {
        user_id: Uuid,
        analysis_kind: &'a str,
        report_type: &'a str,
        period_start: NaiveDate,
        tx_signature: &'a str,
        buyer_wallet: &'a str,
        recipient_wallet: &'a str,
        usdc_mint: &'a str,
        amount_micro: u64,
        network: &'a str,
        resource: &'a str,
        request_hash: &'a str,
        payment_memo: &'a str,
    }

    let url = format!(
        "{}/rest/v1/analysis_payments",
        state.config.supabase_url.trim_end_matches('/')
    );
    let res = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .json(&InsertPayment {
            user_id,
            analysis_kind: charge.analysis_kind.as_str(),
            report_type: charge.report_type.as_str(),
            period_start: charge.period_start,
            tx_signature: &payment.signature,
            buyer_wallet: &payment.buyer_address,
            recipient_wallet: &state.config.solana_platform_wallet,
            usdc_mint: &state.config.solana_usdc_mint,
            amount_micro: charge.amount_micro,
            network: &payment.network,
            resource,
            request_hash,
            payment_memo,
        })
        .send()
        .await
        .context("analysis_payments INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        if body.contains("duplicate") || body.contains("23505") {
            return verify_existing_payment(
                state,
                user_id,
                charge,
                payment,
                resource,
                request_hash,
                payment_memo,
            )
            .await;
        }
        return Err(anyhow!("analysis_payments INSERT 실패: {}", body));
    }

    Ok(())
}

async fn verify_existing_payment(
    state: &AppState,
    user_id: Uuid,
    charge: &AnalysisCharge,
    payment: &PaymentHeaderPayload,
    resource: &str,
    request_hash: &str,
    payment_memo: &str,
) -> Result<()> {
    #[derive(Deserialize)]
    struct ExistingPayment {
        user_id: Uuid,
        analysis_kind: String,
        report_type: String,
        period_start: NaiveDate,
        buyer_wallet: String,
        recipient_wallet: String,
        usdc_mint: String,
        amount_micro: u64,
        network: String,
        resource: String,
        request_hash: String,
        payment_memo: String,
        consumed_at: Option<chrono::DateTime<Utc>>,
    }

    let url = format!(
        "{}/rest/v1/analysis_payments?tx_signature=eq.{}&select=user_id,analysis_kind,report_type,period_start,buyer_wallet,recipient_wallet,usdc_mint,amount_micro,network,resource,request_hash,payment_memo,consumed_at&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        payment.signature
    );
    let rows: Vec<ExistingPayment> = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("analysis_payments duplicate SELECT 요청 실패")?
        .json()
        .await
        .context("analysis_payments duplicate SELECT 응답 파싱 실패")?;

    let existing = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("중복 결제 signature가 있지만 기존 결제 row를 찾을 수 없습니다"))?;

    if existing.user_id == user_id
        && existing.analysis_kind == charge.analysis_kind.as_str()
        && existing.report_type == charge.report_type.as_str()
        && existing.period_start == charge.period_start
        && existing.buyer_wallet == payment.buyer_address
        && existing.recipient_wallet == state.config.solana_platform_wallet
        && existing.usdc_mint == state.config.solana_usdc_mint
        && existing.amount_micro >= charge.amount_micro
        && existing.network == payment.network
        && existing.resource == resource
        && existing.request_hash == request_hash
        && existing.payment_memo == payment_memo
        && existing.consumed_at.is_none()
    {
        return Ok(());
    }

    Err(anyhow!("이미 사용된 결제 signature입니다"))
}

fn request_hash(req: &GenerateReportRequest) -> Result<String> {
    let canonical = serde_json::to_vec(req).context("분석 요청 canonical JSON 생성 실패")?;
    let digest = Sha256::digest(canonical);
    Ok(hex::encode(digest))
}

fn payment_memo(user_id: Uuid, request_hash: &str) -> String {
    format!("spentopia-analysis:{}:{}", user_id, request_hash)
}

fn decode_pubkey(s: &str) -> Result<[u8; 32]> {
    let bytes = bs58::decode(s).into_vec().context("base58 디코딩 실패")?;
    bytes
        .try_into()
        .map_err(|_| anyhow!("pubkey 길이 오류: {}", s))
}

fn derive_associated_token_address(wallet: &str, mint: &str) -> Result<String> {
    let wallet = decode_pubkey(wallet)?;
    let mint = decode_pubkey(mint)?;
    let token_program = decode_pubkey(TOKEN_PROGRAM_ID)?;
    let assoc_program = decode_pubkey(ASSOC_TOKEN_PROGRAM_ID)?;
    let (addr, _) = find_program_address(&[&wallet, &token_program, &mint], &assoc_program);
    Ok(bs58::encode(addr).into_string())
}

fn find_program_address(seeds: &[&[u8]], program_id: &[u8; 32]) -> ([u8; 32], u8) {
    for nonce in (0u8..=255).rev() {
        let mut hasher = Sha256::new();
        for seed in seeds {
            hasher.update(seed);
        }
        hasher.update([nonce]);
        hasher.update(program_id);
        hasher.update(b"ProgramDerivedAddress");
        let hash: [u8; 32] = hasher.finalize().into();
        if ed25519_dalek::VerifyingKey::from_bytes(&hash).is_err() {
            return (hash, nonce);
        }
    }
    panic!("PDA를 찾을 수 없습니다.");
}

fn base64_decode(input: &str) -> Result<Vec<u8>> {
    fn val(b: u8) -> Option<u8> {
        match b {
            b'A'..=b'Z' => Some(b - b'A'),
            b'a'..=b'z' => Some(b - b'a' + 26),
            b'0'..=b'9' => Some(b - b'0' + 52),
            b'+' => Some(62),
            b'/' => Some(63),
            _ => None,
        }
    }

    let bytes: Vec<u8> = input
        .as_bytes()
        .iter()
        .copied()
        .filter(|b| !b.is_ascii_whitespace())
        .collect();
    if bytes.len() % 4 != 0 {
        return Err(anyhow!("base64 길이 오류"));
    }

    let mut out = Vec::with_capacity(bytes.len() / 4 * 3);
    for chunk in bytes.chunks(4) {
        let pad = chunk.iter().rev().take_while(|&&b| b == b'=').count();
        if pad > 2 {
            return Err(anyhow!("base64 padding 오류"));
        }

        let n0 = val(chunk[0]).ok_or_else(|| anyhow!("base64 문자 오류"))? as u32;
        let n1 = val(chunk[1]).ok_or_else(|| anyhow!("base64 문자 오류"))? as u32;
        let n2 = if chunk[2] == b'=' {
            0
        } else {
            val(chunk[2]).ok_or_else(|| anyhow!("base64 문자 오류"))? as u32
        };
        let n3 = if chunk[3] == b'=' {
            0
        } else {
            val(chunk[3]).ok_or_else(|| anyhow!("base64 문자 오류"))? as u32
        };
        let n = (n0 << 18) | (n1 << 12) | (n2 << 6) | n3;

        out.push(((n >> 16) & 0xff) as u8);
        if pad < 2 {
            out.push(((n >> 8) & 0xff) as u8);
        }
        if pad < 1 {
            out.push((n & 0xff) as u8);
        }
    }

    Ok(out)
}
