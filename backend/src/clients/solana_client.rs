// clients/solana_client.rs
//
// solana-sdk 없이 reqwest + sha2 + ed25519-dalek + bs58 만으로
// Solana 온체인 트랜잭션을 직접 구성·서명·전송하는 클라이언트.
//
// 지원 instruction:
//   mint_spt_to_user — 성실도 보상 SPT 민팅 (admin 서명, 백엔드 전용)
//
// 트랜잭션 직렬화 포맷: Solana legacy transaction (v0 아님)
// 인코딩: base64 (sendTransaction encoding: "base64")

use anyhow::{Context, Result, anyhow};
use ed25519_dalek::{Signer, SigningKey};
use sha2::{Digest, Sha256};

// ── 고정 주소 상수 ─────────────────────────────────────────────
const TOKEN_PROGRAM_ID: &str = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
const ASSOC_TOKEN_PROGRAM_ID: &str = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL";
const SYSTEM_PROGRAM_ID: &str = "11111111111111111111111111111111";
const METADATA_PROGRAM_ID: &str = "metaqbxxUerdq28cj1RbAWkYQm3ybzjb6a8bt518x1s";
const SYSVAR_INSTRUCTIONS_ID: &str = "Sysvar1nstructions1111111111111111111111111";

// ── PDA seeds (스마트 컨트랙트 constants.rs와 동일) ───────────────
const PLATFORM_CONFIG_SEED: &[u8] = b"platform_config";
const SPT_TOKEN_MINT_SEED: &[u8] = b"spt_token_mint";
const SPT_TOKEN_AUTHORITY_SEED: &[u8] = b"spt_token_authority";
const LISTING_SEED: &[u8] = b"listing";
const ESCROW_SEED: &[u8] = b"escrow";
const AVATAR_MINT_SEED: &[u8] = b"avatar_mint";
const AVATAR_COLLECTION_MINT_SEED: &[u8] = b"avatar_collection_mint";

// SPT decimals: 1 SPT = 1_000_000 base units
pub const SPT_DECIMALS: u64 = 1_000_000;

// ── 유틸: base58 pubkey → [u8; 32] ───────────────────────────
fn decode_pubkey(s: &str) -> Result<[u8; 32]> {
    let bytes = bs58::decode(s).into_vec().context("base58 디코딩 실패")?;
    bytes
        .try_into()
        .map_err(|_| anyhow!("pubkey 길이 오류: {}", s))
}

// ── 유틸: base64 인코딩 (외부 crate 없이 직접 구현) ──────────────
fn base64_encode(bytes: &[u8]) -> String {
    const TABLE: &[u8] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let mut out = String::with_capacity((bytes.len() + 2) / 3 * 4);
    for chunk in bytes.chunks(3) {
        let b0 = chunk[0] as u32;
        let b1 = if chunk.len() > 1 { chunk[1] as u32 } else { 0 };
        let b2 = if chunk.len() > 2 { chunk[2] as u32 } else { 0 };
        let n = (b0 << 16) | (b1 << 8) | b2;
        out.push(TABLE[((n >> 18) & 63) as usize] as char);
        out.push(TABLE[((n >> 12) & 63) as usize] as char);
        if chunk.len() > 1 {
            out.push(TABLE[((n >> 6) & 63) as usize] as char);
        } else {
            out.push('=');
        }
        if chunk.len() > 2 {
            out.push(TABLE[(n & 63) as usize] as char);
        } else {
            out.push('=');
        }
    }
    out
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

// ── Compact-u16 인코딩 (Solana 트랜잭션 내부 길이 표현 방식) ───────
fn compact_u16(n: usize) -> Vec<u8> {
    let mut out = Vec::new();
    let mut val = n as u16;
    loop {
        let mut b = (val & 0x7f) as u8;
        val >>= 7;
        if val != 0 {
            b |= 0x80;
        }
        out.push(b);
        if val == 0 {
            break;
        }
    }
    out
}

// ── PDA 계산 ──────────────────────────────────────────────────
// SHA256(seed1 || ... || seedN || nonce || program_id || "ProgramDerivedAddress")
// 결과가 ed25519 곡선 위에 없을 때 유효한 PDA
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
        // ed25519 곡선 위의 점이면 유효하지 않은 PDA → 다음 nonce 시도
        if ed25519_dalek::VerifyingKey::from_bytes(&hash).is_err() {
            return (hash, nonce);
        }
    }
    panic!("PDA를 찾을 수 없습니다. seeds가 올바른지 확인하세요.");
}

// ── ATA(Associated Token Account) 주소 계산 ──────────────────
fn get_associated_token_address(wallet: &[u8; 32], mint: &[u8; 32]) -> Result<[u8; 32]> {
    let token_program = decode_pubkey(TOKEN_PROGRAM_ID)?;
    let assoc_program = decode_pubkey(ASSOC_TOKEN_PROGRAM_ID)?;
    let (addr, _) = find_program_address(&[wallet, &token_program, mint], &assoc_program);
    Ok(addr)
}

// ── Anchor instruction discriminator ──────────────────────────
// SHA256("global:{name}")[..8]
fn anchor_discriminator(name: &str) -> [u8; 8] {
    let hash = Sha256::digest(format!("global:{}", name).as_bytes());
    hash[..8].try_into().unwrap()
}

fn push_anchor_string(out: &mut Vec<u8>, value: &str) -> Result<()> {
    let len: u32 = value
        .len()
        .try_into()
        .map_err(|_| anyhow!("Anchor string too long"))?;
    out.extend_from_slice(&len.to_le_bytes());
    out.extend_from_slice(value.as_bytes());
    Ok(())
}

pub fn derive_listing_address(
    seller_wallet_b58: &str,
    nft_mint_b58: &str,
    program_id_str: &str,
) -> Result<String> {
    let seller = decode_pubkey(seller_wallet_b58)?;
    let nft_mint = decode_pubkey(nft_mint_b58)?;
    let program_id = decode_pubkey(program_id_str)?;
    let (listing, _) = find_program_address(
        &[LISTING_SEED, seller.as_ref(), nft_mint.as_ref()],
        &program_id,
    );
    Ok(bs58::encode(listing).into_string())
}

pub fn derive_escrow_address(listing_b58: &str, program_id_str: &str) -> Result<String> {
    let listing = decode_pubkey(listing_b58)?;
    let program_id = decode_pubkey(program_id_str)?;
    let (escrow, _) = find_program_address(&[ESCROW_SEED, listing.as_ref()], &program_id);
    Ok(bs58::encode(escrow).into_string())
}

async fn get_account_data(
    rpc_url: &str,
    client: &reqwest::Client,
    account_b58: &str,
) -> Result<Option<Vec<u8>>> {
    let res: serde_json::Value = client
        .post(rpc_url)
        .json(&serde_json::json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "getAccountInfo",
            "params": [
                account_b58,
                {"encoding": "base64", "commitment": "confirmed"}
            ]
        }))
        .send()
        .await
        .context("getAccountInfo 요청 실패")?
        .json()
        .await
        .context("getAccountInfo 파싱 실패")?;

    if let Some(err) = res.get("error") {
        return Err(anyhow!("getAccountInfo 실패: {}", err));
    }

    if res["result"]["value"].is_null() {
        return Ok(None);
    }

    let data_b64 = res["result"]["value"]["data"]
        .as_array()
        .and_then(|arr| arr.first())
        .and_then(|v| v.as_str())
        .ok_or_else(|| anyhow!("getAccountInfo data 필드 없음"))?;

    Ok(Some(base64_decode(data_b64)?))
}

pub async fn account_exists(
    rpc_url: &str,
    client: &reqwest::Client,
    account_b58: &str,
) -> Result<bool> {
    Ok(get_account_data(rpc_url, client, account_b58)
        .await?
        .is_some())
}

pub async fn get_spl_token_account_amount(
    rpc_url: &str,
    client: &reqwest::Client,
    token_account_b58: &str,
) -> Result<Option<u64>> {
    let Some(data) = get_account_data(rpc_url, client, token_account_b58).await? else {
        return Ok(None);
    };

    let amount_bytes: [u8; 8] = data
        .get(64..72)
        .ok_or_else(|| anyhow!("SPL token account amount 데이터 길이 부족"))?
        .try_into()
        .map_err(|_| anyhow!("SPL token account amount 파싱 실패"))?;

    Ok(Some(u64::from_le_bytes(amount_bytes)))
}

pub async fn get_platform_fee_rate(
    rpc_url: &str,
    client: &reqwest::Client,
    program_id_str: &str,
) -> Result<u16> {
    let program_id = decode_pubkey(program_id_str)?;
    let (platform_config, _) = find_program_address(&[PLATFORM_CONFIG_SEED], &program_id);
    let platform_config_b58 = bs58::encode(platform_config).into_string();

    let res: serde_json::Value = client
        .post(rpc_url)
        .json(&serde_json::json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "getAccountInfo",
            "params": [
                platform_config_b58,
                {"encoding": "base64", "commitment": "confirmed"}
            ]
        }))
        .send()
        .await
        .context("platform_config getAccountInfo 요청 실패")?
        .json()
        .await
        .context("platform_config getAccountInfo 파싱 실패")?;

    if let Some(err) = res.get("error") {
        return Err(anyhow!("platform_config getAccountInfo 실패: {}", err));
    }

    let data_b64 = res["result"]["value"]["data"]
        .as_array()
        .and_then(|arr| arr.first())
        .and_then(|v| v.as_str())
        .ok_or_else(|| anyhow!("platform_config data 필드 없음"))?;
    let data = base64_decode(data_b64)?;

    // PlatformConfig layout: discriminator(8) + admin(32) + fee_rate(u16 LE) + ...
    let fee_offset = 8 + 32;
    let fee_bytes: [u8; 2] = data
        .get(fee_offset..fee_offset + 2)
        .ok_or_else(|| anyhow!("platform_config fee_rate 데이터 길이 부족"))?
        .try_into()
        .map_err(|_| anyhow!("platform_config fee_rate 파싱 실패"))?;

    Ok(u16::from_le_bytes(fee_bytes))
}

// ── RPC: getSignatureStatuses ─────────────────────────────────
//
// required_status: "confirmed" 또는 "finalized"
//   - confirmed: 슈퍼마조리티 투표 66% 이상, ~2-4초 (admin 민팅 후 DB 기록 용)
//   - finalized: 슈퍼마조리티 투표 완료 + 불가역, ~20-30초 (유저 제출 tx 검증 용)
async fn check_signature_status(
    rpc_url: &str,
    client: &reqwest::Client,
    signature: &str,
    required_status: &str,
) -> Result<()> {
    let response = client
        .post(rpc_url)
        .json(&serde_json::json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "getSignatureStatuses",
            "params": [[signature], {"searchTransactionHistory": true}]
        }))
        .send()
        .await
        .context("getSignatureStatuses 요청 실패")?;

    if !response.status().is_success() {
        let status = response.status();
        let body = response.text().await.unwrap_or_default();
        return Err(anyhow!(
            "getSignatureStatuses 오류 (HTTP {}): {}",
            status,
            body
        ));
    }

    let res: serde_json::Value = response
        .json()
        .await
        .context("getSignatureStatuses 파싱 실패")?;

    let status = res["result"]["value"]
        .as_array()
        .and_then(|arr| arr.first())
        .ok_or_else(|| anyhow!("트랜잭션 상태를 찾을 수 없습니다: {}", signature))?;

    if status.is_null() {
        return Err(anyhow!(
            "트랜잭션이 아직 처리되지 않았습니다: {}",
            signature
        ));
    }
    if !status["err"].is_null() {
        return Err(anyhow!("트랜잭션 실패 확인됨: {}", status["err"]));
    }

    let confirmation = status["confirmationStatus"].as_str().unwrap_or("");
    let is_sufficient = match required_status {
        "confirmed" => matches!(confirmation, "confirmed" | "finalized"),
        "finalized" => confirmation == "finalized",
        other => return Err(anyhow!("알 수 없는 required_status: {}", other)),
    };

    if !is_sufficient {
        return Err(anyhow!(
            "트랜잭션이 아직 {} 상태가 아닙니다 (현재: {})",
            required_status,
            confirmation
        ));
    }

    Ok(())
}

// admin이 직접 서명·전송한 트랜잭션 확인용 (confirmed 이상이면 충분)
pub async fn check_signature_confirmed(
    rpc_url: &str,
    client: &reqwest::Client,
    signature: &str,
) -> Result<()> {
    check_signature_status(rpc_url, client, signature, "confirmed").await
}

// 유저 제출 트랜잭션 최종 확인용 (불가역 보장)
async fn check_signature_finalized(
    rpc_url: &str,
    client: &reqwest::Client,
    signature: &str,
) -> Result<()> {
    check_signature_status(rpc_url, client, signature, "finalized").await
}

// ── Helius Enhanced Transaction API ───────────────────────────
//
// Helius가 파싱한 트랜잭션을 가져온다.
// 기존 raw JSON-RPC getTransaction 대비 장점:
//   - transactionError 필드로 성공/실패 즉시 판별
//   - instructions[].accounts 가 flat pubkey 문자열 배열 → 별도 파싱 불필요
//   - 커스텀 Anchor 프로그램도 discriminator 기반으로 정확하게 검증 가능
// rpc_url에 "devnet"이 포함되면 devnet Enhanced API, 아니면 mainnet 사용
fn helius_enhanced_base(rpc_url: &str) -> &'static str {
    if rpc_url.contains("devnet") {
        "https://api-devnet.helius-rpc.com"
    } else {
        "https://api-mainnet.helius-rpc.com"
    }
}

async fn get_helius_transaction(
    rpc_url: &str,
    helius_api_key: &str,
    client: &reqwest::Client,
    signature: &str,
) -> Result<serde_json::Value> {
    let url = format!(
        "{}/v0/transactions?api-key={}",
        helius_enhanced_base(rpc_url),
        helius_api_key
    );
    let mut last_empty_response = false;

    for attempt in 0..4 {
        let response = client
            .post(&url)
            .json(&serde_json::json!({ "transactions": [signature] }))
            .send()
            .await
            .context("Helius API 요청 실패")?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            return Err(anyhow!("Helius API 오류 (HTTP {}): {}", status, body));
        }

        let res: Vec<serde_json::Value> =
            response.json().await.context("Helius API 응답 파싱 실패")?;

        if let Some(tx) = res.into_iter().next() {
            if !tx["transactionError"].is_null() {
                return Err(anyhow!("실패한 트랜잭션입니다: {}", tx["transactionError"]));
            }
            return Ok(tx);
        }

        last_empty_response = true;
        if attempt < 3 {
            tokio::time::sleep(std::time::Duration::from_millis(700)).await;
        }
    }

    if last_empty_response {
        return Err(anyhow!(
            "트랜잭션 파싱 결과가 아직 준비되지 않았습니다: {}",
            signature
        ));
    }

    Err(anyhow!("트랜잭션을 찾을 수 없습니다: {}", signature))
}

// ── 트랜잭션 검증 (Helius Enhanced API 사용) ─────────────────
//
// 온체인 트랜잭션이 실제로 Spentopia 프로그램의 특정 instruction을
// 포함하는지, 그리고 필수 계정이 모두 존재하는지 검증한다.
//
// rpc_url: Helius RPC URL — finalized 상태 최종 확인에 사용 (config.solana_rpc_url)
// helius_api_key: Helius API 키 — Enhanced API 트랜잭션 파싱에 사용 (config.helius_api_key)
// instruction_name: Anchor instruction 이름 (discriminator 계산에 사용)
// required_pubkeys: 트랜잭션에 반드시 포함돼야 할 pubkey 목록
// required_signer: feePayer로 검증할 서명자 pubkey (None이면 생략)
// expected_account_positions: instruction accounts 배열에서 특정 위치의 pubkey 검증
pub async fn verify_program_instruction_tx_data(
    rpc_url: &str,
    helius_api_key: &str,
    client: &reqwest::Client,
    signature: &str,
    program_id: &str,
    instruction_name: &str,
    required_pubkeys: &[&str],
    required_signer: Option<&str>,
    expected_account_positions: Option<&[(usize, &str)]>,
) -> Result<Vec<u8>> {
    if signature.trim().is_empty() {
        return Err(anyhow!("tx_signature 누락"));
    }

    check_signature_confirmed(rpc_url, client, signature).await?;
    let tx = get_helius_transaction(rpc_url, helius_api_key, client, signature).await?;

    // feePayer 기반 서명자 확인
    if let Some(signer) = required_signer {
        let fee_payer = tx["feePayer"].as_str().unwrap_or("");
        if fee_payer != signer {
            return Err(anyhow!(
                "필수 서명자(feePayer) 불일치: expected={}, got={}",
                signer,
                fee_payer
            ));
        }
    }

    let instructions = tx["instructions"]
        .as_array()
        .ok_or_else(|| anyhow!("instructions 필드 없음"))?;

    let discriminator = anchor_discriminator(instruction_name);

    // Spentopia instruction 탐색
    let instruction = instructions
        .iter()
        .find(|ix| {
            if ix["programId"].as_str() != Some(program_id) {
                return false;
            }
            let data_bytes = ix["data"]
                .as_str()
                .and_then(|s| bs58::decode(s).into_vec().ok())
                .unwrap_or_default();
            data_bytes.len() >= 8 && data_bytes[..8] == discriminator
        })
        .ok_or_else(|| {
            anyhow!(
                "Spentopia instruction을 찾을 수 없습니다: {}",
                instruction_name
            )
        })?;

    let data_bytes = instruction["data"]
        .as_str()
        .and_then(|s| bs58::decode(s).into_vec().ok())
        .ok_or_else(|| anyhow!("instruction data 디코딩 실패"))?;

    // Helius에서 accounts는 flat pubkey 문자열 배열
    let accounts: Vec<&str> = instruction["accounts"]
        .as_array()
        .ok_or_else(|| anyhow!("accounts 필드 없음"))?
        .iter()
        .filter_map(|v| v.as_str())
        .collect();

    // 필수 pubkey 존재 확인
    for required in required_pubkeys {
        if !accounts.contains(required) {
            return Err(anyhow!("필수 계정이 트랜잭션에 없습니다: {}", required));
        }
    }

    // 특정 위치의 계정 일치 확인
    if let Some(positions) = expected_account_positions {
        for (index, expected) in positions {
            let actual = accounts
                .get(*index)
                .ok_or_else(|| anyhow!("accounts[{}] 없음 (총 {}개)", index, accounts.len()))?;
            if *actual != *expected {
                return Err(anyhow!(
                    "accounts[{}] 불일치: expected={}, got={}",
                    index,
                    expected,
                    actual
                ));
            }
        }
    }

    Ok(data_bytes)
}

pub async fn verify_program_instruction_tx(
    rpc_url: &str,
    helius_api_key: &str,
    client: &reqwest::Client,
    signature: &str,
    program_id: &str,
    instruction_name: &str,
    required_pubkeys: &[&str],
    required_signer: Option<&str>,
    expected_account_positions: Option<&[(usize, &str)]>,
) -> Result<()> {
    verify_program_instruction_tx_data(
        rpc_url,
        helius_api_key,
        client,
        signature,
        program_id,
        instruction_name,
        required_pubkeys,
        required_signer,
        expected_account_positions,
    )
    .await?;

    Ok(())
}

pub async fn verify_program_instruction_tx_u64_arg(
    rpc_url: &str,
    helius_api_key: &str,
    client: &reqwest::Client,
    signature: &str,
    program_id: &str,
    instruction_name: &str,
    required_pubkeys: &[&str],
    required_signer: Option<&str>,
    expected_account_positions: Option<&[(usize, &str)]>,
) -> Result<u64> {
    let data = verify_program_instruction_tx_data(
        rpc_url,
        helius_api_key,
        client,
        signature,
        program_id,
        instruction_name,
        required_pubkeys,
        required_signer,
        expected_account_positions,
    )
    .await?;

    let value_bytes: [u8; 8] = data
        .get(8..16)
        .ok_or_else(|| anyhow!("{} u64 인자 데이터 길이 부족", instruction_name))?
        .try_into()
        .map_err(|_| anyhow!("{} u64 인자 파싱 실패", instruction_name))?;
    Ok(u64::from_le_bytes(value_bytes))
}

// ── RPC: getLatestBlockhash ────────────────────────────────────
async fn get_latest_blockhash(rpc_url: &str, client: &reqwest::Client) -> Result<[u8; 32]> {
    let res: serde_json::Value = client
        .post(rpc_url)
        .json(&serde_json::json!({
            "jsonrpc": "2.0", "id": 1,
            "method": "getLatestBlockhash",
            "params": [{"commitment": "finalized"}]
        }))
        .send()
        .await
        .context("getLatestBlockhash 요청 실패")?
        .json()
        .await
        .context("getLatestBlockhash 파싱 실패")?;

    let hash_str = res["result"]["value"]["blockhash"]
        .as_str()
        .ok_or_else(|| anyhow!("blockhash 필드 없음"))?;

    bs58::decode(hash_str)
        .into_vec()
        .context("blockhash base58 디코딩 실패")?
        .try_into()
        .map_err(|_| anyhow!("blockhash 길이 오류"))
}

// ── RPC: sendTransaction ───────────────────────────────────────
async fn send_transaction(
    rpc_url: &str,
    client: &reqwest::Client,
    tx_bytes: &[u8],
) -> Result<String> {
    let res: serde_json::Value = client
        .post(rpc_url)
        .json(&serde_json::json!({
            "jsonrpc": "2.0", "id": 1,
            "method": "sendTransaction",
            "params": [base64_encode(tx_bytes), {"encoding": "base64", "skipPreflight": false}]
        }))
        .send()
        .await
        .context("sendTransaction 요청 실패")?
        .json()
        .await
        .context("sendTransaction 파싱 실패")?;

    if let Some(err) = res.get("error") {
        return Err(anyhow!("sendTransaction 실패: {}", err));
    }

    res["result"]
        .as_str()
        .map(|s| s.to_string())
        .ok_or_else(|| anyhow!("tx signature 필드 없음"))
}

// ── Solana legacy 트랜잭션 직렬화 ─────────────────────────────
// 계정 순서 (message 헤더 기준):
//   0: admin (writable, signer)
//   1: platform_config (writable)
//   2: spt_token_mint (writable)
//   3: user_token_account (writable)
//   4: user_wallet (readonly)
//   5: spt_token_authority (readonly)
//   6: token_program (readonly)
//   7: associated_token_program (readonly)
//   8: system_program (readonly)
//   9: spentopia_program (readonly, program)
fn build_tx(
    admin_pubkey: &[u8; 32],
    platform_config: &[u8; 32],
    spt_token_mint: &[u8; 32],
    user_token_account: &[u8; 32],
    user_wallet: &[u8; 32],
    spt_token_authority: &[u8; 32],
    program_id: &[u8; 32],
    recent_blockhash: &[u8; 32],
    ix_data: &[u8],
    signing_key: &SigningKey,
) -> Vec<u8> {
    let token_program = decode_pubkey(TOKEN_PROGRAM_ID).unwrap();
    let assoc_token_prog = decode_pubkey(ASSOC_TOKEN_PROGRAM_ID).unwrap();
    let system_program = decode_pubkey(SYSTEM_PROGRAM_ID).unwrap();

    // accounts 배열 (정렬 순서 엄격히 유지)
    let accounts: &[[u8; 32]] = &[
        *admin_pubkey,        // 0: writable signer
        *platform_config,     // 1: writable
        *spt_token_mint,      // 2: writable
        *user_token_account,  // 3: writable
        *user_wallet,         // 4: readonly
        *spt_token_authority, // 5: readonly
        token_program,        // 6: readonly
        assoc_token_prog,     // 7: readonly
        system_program,       // 8: readonly
        *program_id,          // 9: readonly (program)
    ];

    // instruction account indexes (MintSptToUser Accounts 구조체 순서와 동일)
    let ix_accounts: &[u8] = &[
        1, // platform_config
        0, // admin
        4, // user
        2, // spt_token_mint
        5, // spt_token_authority
        3, // user_token_account
        6, // token_program
        7, // associated_token_program
        8, // system_program
    ];

    // Message 직렬화
    let mut msg = Vec::new();
    // Header: num_required_signatures=1, num_readonly_signed=0, num_readonly_unsigned=6
    msg.extend_from_slice(&[1u8, 0u8, 6u8]);
    // Accounts
    msg.extend(compact_u16(accounts.len()));
    for acc in accounts {
        msg.extend_from_slice(acc);
    }
    // Recent blockhash
    msg.extend_from_slice(recent_blockhash);
    // Instructions (1개)
    msg.extend(compact_u16(1));
    msg.push(9u8); // program_id_index
    msg.extend(compact_u16(ix_accounts.len()));
    msg.extend_from_slice(ix_accounts);
    msg.extend(compact_u16(ix_data.len()));
    msg.extend_from_slice(ix_data);

    // 서명
    let sig = signing_key.sign(&msg);
    let sig_bytes = sig.to_bytes();

    // 트랜잭션: [compact_u16(1 sig), sig(64), message]
    let mut tx = Vec::new();
    tx.extend(compact_u16(1));
    tx.extend_from_slice(&sig_bytes);
    tx.extend_from_slice(&msg);
    tx
}

fn build_mint_avatar_tx(
    admin_pubkey: &[u8; 32],
    platform_config: &[u8; 32],
    user_wallet: &[u8; 32],
    avatar_mint: &[u8; 32],
    metadata: &[u8; 32],
    master_edition: &[u8; 32],
    spt_token_authority: &[u8; 32],
    user_token_account: &[u8; 32],
    collection_mint: &[u8; 32],
    collection_metadata: &[u8; 32],
    collection_master_edition: &[u8; 32],
    program_id: &[u8; 32],
    recent_blockhash: &[u8; 32],
    ix_data: &[u8],
    signing_key: &SigningKey,
) -> Vec<u8> {
    let metadata_program = decode_pubkey(METADATA_PROGRAM_ID).unwrap();
    let sysvar_instructions = decode_pubkey(SYSVAR_INSTRUCTIONS_ID).unwrap();
    let token_program = decode_pubkey(TOKEN_PROGRAM_ID).unwrap();
    let assoc_token_prog = decode_pubkey(ASSOC_TOKEN_PROGRAM_ID).unwrap();
    let system_program = decode_pubkey(SYSTEM_PROGRAM_ID).unwrap();

    let accounts: &[[u8; 32]] = &[
        *admin_pubkey,              // 0 writable signer
        *avatar_mint,               // 1 writable
        *metadata,                  // 2 writable
        *master_edition,            // 3 writable
        *spt_token_authority,       // 4 writable
        *user_token_account,        // 5 writable
        *collection_metadata,       // 6 writable
        *platform_config,           // 7 readonly
        *user_wallet,               // 8 readonly
        *collection_mint,           // 9 readonly
        *collection_master_edition, // 10 readonly
        metadata_program,           // 11 readonly
        sysvar_instructions,        // 12 readonly
        token_program,              // 13 readonly
        assoc_token_prog,           // 14 readonly
        system_program,             // 15 readonly
        *program_id,                // 16 readonly program
    ];

    let ix_accounts: &[u8] = &[
        7,  // platform_config
        0,  // admin
        8,  // user
        1,  // avatar_mint
        2,  // metadata
        3,  // master_edition
        4,  // spt_token_authority
        5,  // user_token_account
        9,  // collection_mint
        6,  // collection_metadata
        10, // collection_master_edition
        11, // metadata_program
        12, // sysvar_instructions
        13, // token_program
        14, // associated_token_program
        15, // system_program
    ];

    let mut msg = Vec::new();
    msg.extend_from_slice(&[1u8, 0u8, 10u8]);
    msg.extend(compact_u16(accounts.len()));
    for acc in accounts {
        msg.extend_from_slice(acc);
    }
    msg.extend_from_slice(recent_blockhash);
    msg.extend(compact_u16(1));
    msg.push(16u8);
    msg.extend(compact_u16(ix_accounts.len()));
    msg.extend_from_slice(ix_accounts);
    msg.extend(compact_u16(ix_data.len()));
    msg.extend_from_slice(ix_data);

    let sig = signing_key.sign(&msg);
    let mut tx = Vec::new();
    tx.extend(compact_u16(1));
    tx.extend_from_slice(&sig.to_bytes());
    tx.extend_from_slice(&msg);
    tx
}

// ── 공개 함수: mint_spt_to_user ────────────────────────────────
//
// 성실도 보상 및 출금 시 백엔드에서 호출.
// admin 키페어로 서명하여 user_wallet에 SPT amount(base units) 민팅.
//
// keypair_b58: base58 인코딩된 64바이트 ed25519 키페어
// amount: base units (1 SPT = SPT_DECIMALS = 1_000_000)
pub async fn mint_spt_to_user(
    rpc_url: &str,
    client: &reqwest::Client,
    keypair_b58: &str,
    user_wallet_b58: &str,
    program_id_str: &str,
    amount: u64,
) -> Result<String> {
    // 키페어 파싱
    let keypair_bytes = bs58::decode(keypair_b58)
        .into_vec()
        .context("admin 키페어 base58 디코딩 실패")?;
    let keypair_arr: [u8; 64] = keypair_bytes
        .try_into()
        .map_err(|_| anyhow!("admin 키페어는 64바이트여야 합니다"))?;
    let signing_key =
        SigningKey::from_keypair_bytes(&keypair_arr).context("admin 키페어 파싱 실패")?;
    let admin_pubkey: [u8; 32] = signing_key.verifying_key().to_bytes();

    // 주소 파싱
    let user_wallet = decode_pubkey(user_wallet_b58)?;
    let program_id = decode_pubkey(program_id_str)?;

    // PDAs
    let (platform_config, _) = find_program_address(&[PLATFORM_CONFIG_SEED], &program_id);
    let (spt_token_mint, _) = find_program_address(&[SPT_TOKEN_MINT_SEED], &program_id);
    let (spt_token_authority, _) = find_program_address(&[SPT_TOKEN_AUTHORITY_SEED], &program_id);

    // ATA
    let user_token_account = get_associated_token_address(&user_wallet, &spt_token_mint)?;

    // Instruction data: discriminator(8) + amount(u64 LE)
    let mut ix_data = Vec::with_capacity(16);
    ix_data.extend_from_slice(&anchor_discriminator("mint_spt_to_user"));
    ix_data.extend_from_slice(&amount.to_le_bytes());

    // Blockhash 조회
    let blockhash = get_latest_blockhash(rpc_url, client).await?;

    // 트랜잭션 빌드 & 서명
    let tx = build_tx(
        &admin_pubkey,
        &platform_config,
        &spt_token_mint,
        &user_token_account,
        &user_wallet,
        &spt_token_authority,
        &program_id,
        &blockhash,
        &ix_data,
        &signing_key,
    );

    // 전송
    let signature = send_transaction(rpc_url, client, &tx).await?;
    tracing::info!(
        "mint_spt_to_user 성공 | user: {} | amount: {} | tx: {}",
        user_wallet_b58,
        amount,
        signature
    );
    Ok(signature)
}

pub async fn mint_avatar_nft_to_user(
    rpc_url: &str,
    client: &reqwest::Client,
    keypair_b58: &str,
    user_wallet_b58: &str,
    program_id_str: &str,
    mint_seed: &str,
    name: &str,
    symbol: &str,
    uri: &str,
) -> Result<(String, String)> {
    if mint_seed.as_bytes().len() > 32 {
        return Err(anyhow!("avatar mint_seed는 32바이트 이하여야 합니다"));
    }

    let keypair_bytes = bs58::decode(keypair_b58)
        .into_vec()
        .context("admin 키페어 base58 디코딩 실패")?;
    let keypair_arr: [u8; 64] = keypair_bytes
        .try_into()
        .map_err(|_| anyhow!("admin 키페어는 64바이트여야 합니다"))?;
    let signing_key =
        SigningKey::from_keypair_bytes(&keypair_arr).context("admin 키페어 파싱 실패")?;
    let admin_pubkey: [u8; 32] = signing_key.verifying_key().to_bytes();

    let user_wallet = decode_pubkey(user_wallet_b58)?;
    let program_id = decode_pubkey(program_id_str)?;
    let metadata_program = decode_pubkey(METADATA_PROGRAM_ID)?;

    let (platform_config, _) = find_program_address(&[PLATFORM_CONFIG_SEED], &program_id);
    let (spt_token_authority, _) = find_program_address(&[SPT_TOKEN_AUTHORITY_SEED], &program_id);
    let (avatar_mint, _) = find_program_address(
        &[AVATAR_MINT_SEED, user_wallet.as_ref(), mint_seed.as_bytes()],
        &program_id,
    );
    let (collection_mint, _) = find_program_address(&[AVATAR_COLLECTION_MINT_SEED], &program_id);
    let (metadata, _) = find_program_address(
        &[b"metadata", metadata_program.as_ref(), avatar_mint.as_ref()],
        &metadata_program,
    );
    let (master_edition, _) = find_program_address(
        &[
            b"metadata",
            metadata_program.as_ref(),
            avatar_mint.as_ref(),
            b"edition",
        ],
        &metadata_program,
    );
    let (collection_metadata, _) = find_program_address(
        &[
            b"metadata",
            metadata_program.as_ref(),
            collection_mint.as_ref(),
        ],
        &metadata_program,
    );
    let (collection_master_edition, _) = find_program_address(
        &[
            b"metadata",
            metadata_program.as_ref(),
            collection_mint.as_ref(),
            b"edition",
        ],
        &metadata_program,
    );
    let user_token_account = get_associated_token_address(&user_wallet, &avatar_mint)?;

    let mut ix_data = Vec::new();
    ix_data.extend_from_slice(&anchor_discriminator("mint_avatar_nft"));
    push_anchor_string(&mut ix_data, mint_seed)?;
    push_anchor_string(&mut ix_data, name)?;
    push_anchor_string(&mut ix_data, symbol)?;
    push_anchor_string(&mut ix_data, uri)?;

    let blockhash = get_latest_blockhash(rpc_url, client).await?;
    let tx = build_mint_avatar_tx(
        &admin_pubkey,
        &platform_config,
        &user_wallet,
        &avatar_mint,
        &metadata,
        &master_edition,
        &spt_token_authority,
        &user_token_account,
        &collection_mint,
        &collection_metadata,
        &collection_master_edition,
        &program_id,
        &blockhash,
        &ix_data,
        &signing_key,
    );

    let signature = send_transaction(rpc_url, client, &tx).await?;
    let mint_address = bs58::encode(avatar_mint).into_string();
    tracing::info!(
        "mint_avatar_nft 성공 | user: {} | mint_seed: {} | mint: {} | tx: {}",
        user_wallet_b58,
        mint_seed,
        mint_address,
        signature
    );
    Ok((signature, mint_address))
}

pub async fn get_collection_assets_by_owner(
    rpc_url: &str,
    client: &reqwest::Client,
    owner_wallet_b58: &str,
    collection_mint_b58: &str,
) -> Result<Vec<serde_json::Value>> {
    let response = client
        .post(rpc_url)
        .json(&serde_json::json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "getAssetsByOwner",
            "params": {
                "ownerAddress": owner_wallet_b58,
                "page": 1,
                "limit": 1000,
                "displayOptions": {
                    "showCollectionMetadata": true
                }
            }
        }))
        .send()
        .await
        .context("getAssetsByOwner 요청 실패")?;

    if !response.status().is_success() {
        let status = response.status();
        let body = response.text().await.unwrap_or_default();
        return Err(anyhow!("getAssetsByOwner 오류 (HTTP {}): {}", status, body));
    }

    let res: serde_json::Value = response
        .json()
        .await
        .context("getAssetsByOwner 응답 파싱 실패")?;

    if let Some(err) = res.get("error") {
        return Err(anyhow!("getAssetsByOwner RPC 오류: {}", err));
    }

    let items = res["result"]["items"]
        .as_array()
        .cloned()
        .unwrap_or_default();

    let filtered = items
        .into_iter()
        .filter(|asset| {
            asset["grouping"].as_array().is_some_and(|groups| {
                groups.iter().any(|group| {
                    group["group_key"].as_str() == Some("collection")
                        && group["group_value"].as_str() == Some(collection_mint_b58)
                })
            })
        })
        .collect();

    Ok(filtered)
}
