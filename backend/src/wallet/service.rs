// wallet/service.rs
//
// 지갑 연동/해제 비즈니스 로직
//
// ■ link_wallet   → 로그인된 유저의 계정에 지갑 주소를 연결
// ■ unlink_wallet → 연결된 지갑 주소를 해제 (wallet_address = null)
//
// ■ 지갑 연동에도 서명 검증이 필요한 이유
//  "이 지갑이 정말 내 것"임을 증명해야 하기 떄문.
//  서명 없이 주소만 입력하면 남의 지갑 주소를 도용할 수 있음.
//  그래서 연동 전에 /auth/wallet/nonce로 nonce를 먼저 받아야 함.

use anyhow::{Context, Result, anyhow};

// Uuid: 유저 고유 식별자 타입 (예: "550e8400-e29b-41d4-a716-446655440000")
use uuid::Uuid;

use crate::state::AppState;
use chrono::Utc;
use std::time::SystemTime;

// find_user_by_wallet: wallet_address로 DB에서 유저를 조회하는 함수 (auth/service.rs)
// 중복 연동 체크 시 재사용
use crate::auth::service::{find_user_by_wallet, verify_solana_signature_pub};

// link_wallet - 지갑 연동
//
// ■ 처리 순서
//  1) nonce 검증 → 1단계에서 발급한 nonce가 맞는지 확인
//  2) 서명 검증 → 실제로 이 지갑의 주인인지 확인
//  3) 중복 체크 → 이 지갑이 이미 다른 계정에 연동됐는지 확인
//  4) DB 업데이트 → public.users.wallet_address 컬럼 업데이트
pub async fn link_wallet(
    state: &AppState,
    // jwt_middleware가 검증한 로그인된 유저의 id
    user_id: Uuid,
    // 연동할 지갑 주소
    wallet_address: &str,
    // /auth/wallet/nonce에서 발급받은 nonce
    nonce: &str,
    // nonce를 지갑으로 서명한 값
    signature: &str,
) -> Result<()> {
    tracing::info!(
        "[지갑연동] link_wallet 시작: user_id={}, wallet={}, nonce_len={}",
        user_id,
        wallet_address,
        nonce.len()
    );

    // 1) nonce 검증
    // nonce_store에서 이 지갑 주소에 해당하는 nonce를 꺼냄
    // ok_or_else: None이면 에러로 변환
    // None인 경우 → /auth/wallet/nonce를 먼저 호출하지 않은 것
    let entry = state
        .nonce_store
        .get(wallet_address)
        .ok_or_else(|| anyhow!("nonce가 없거나 만료되었습니다. 다시 시도해 주세요."))?;
    tracing::debug!("[지갑연동] nonce_store 에서 nonce 확인");

    // TTL 체크: 발급 후 5분이 지났으면 만료 처리
    if SystemTime::now() > entry.expires_at {
        drop(entry);
        state.nonce_store.remove(wallet_address);
        return Err(anyhow!("nonce가 만료되었습니다. 다시 시도해 주세요."));
    }

    if entry.nonce != nonce {
        tracing::warn!(
            "[지갑연동] nonce 불일치: stored_len={}, received_len={}",
            entry.nonce.len(),
            nonce.len()
        );
        return Err(anyhow!("nonce가 일치하지 않습니다."));
    }
    tracing::debug!("[지갑연동] nonce 일치 확인, 서명 검증 시작");

    // nonce는 1회용 → 검증 즉시 삭제
    // drop() 먼저: DashMap 읽기 잠금 해제 후 remove() 해야 함
    drop(entry);
    state.nonce_store.remove(wallet_address);

    // 2) 서명 검증
    // auth/service.rs의 verify_solana_signature을 pub 래퍼로 재사용
    verify_solana_signature_pub(wallet_address, nonce, signature)?;

    tracing::info!("지갑 연동 서명 검증 성공: wallet={}", wallet_address);

    // 3) 중복 체크
    // 이 지갑이 이미 다른 계정에 연동돼있는지 확인
    // find_user_by_wallet이 Ok → 이미 연동된 지갑 → 에러 반환
    // find_user_by_wallet이 Err → 아직 아무도 안 썼음 → 연동 진행
    if let Ok(_existing_user_id) = find_user_by_wallet(state, wallet_address).await {
        // 이미 연동됐는데 내 계정이라면 재연동이므로 에러 처리 방식은 기획에 따라 다름
        // 일단 "이미 연동된 지갑"으로 막음
        return Err(anyhow!("이미 다른 계정에 연동된 지갑 주소입니다."));
    }

    // 4) DB 업데이트
    // PATCH /reset/v1/users?id=eq.{user_id}
    // body: { "wallet_address": "7xKXt..." }
    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let resp = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        // Prefer: return = minimal → 업데이트 후 row를 반환하지 않음 (불필요하므로 성능 절약)
        .header("Prefer", "return=minimal")
        .json(&serde_json::json!({
            "wallet_address": wallet_address,
            "wallet_connected_at": Utc::now().to_rfc3339(),
        }))
        .send()
        .await
        .context("지갑 연동 DB 업데이트 HTTP 요청 실패")?;

    if !resp.status().is_success() {
        let err_text = resp.text().await.unwrap_or_default();
        return Err(anyhow!("지갑 연동 DB 업데이트 실패: {}", err_text));
    }

    tracing::info!(
        "지갑 연동 완료: user_id={}, wallet={}",
        user_id,
        wallet_address
    );
    Ok(())
}

fn verify_wallet_nonce_and_signature(
    state: &AppState,
    wallet_address: &str,
    nonce: &str,
    signature: &str,
) -> Result<()> {
    let entry = state
        .nonce_store
        .get(wallet_address)
        .ok_or_else(|| anyhow!("nonce가 없거나 만료되었습니다. 다시 시도해 주세요."))?;

    if SystemTime::now() > entry.expires_at {
        drop(entry);
        state.nonce_store.remove(wallet_address);
        return Err(anyhow!("nonce가 만료되었습니다. 다시 시도해 주세요."));
    }

    if entry.nonce != nonce {
        return Err(anyhow!("nonce가 일치하지 않습니다."));
    }

    drop(entry);
    state.nonce_store.remove(wallet_address);

    verify_solana_signature_pub(wallet_address, nonce, signature)?;

    Ok(())
}

// unlink_wallet - 지갑 해제
//
// ■ 처리 순서
//  1) 연동 여부 확인 → 실제로 지갑이 연동된 유저인지 체크
//  2) 요청 지갑 주소가 DB에 연동된 주소와 일치하는지 확인
//  3) nonce + 서명 검증 → access token만으로 해제하지 못하게 함
//  4) DB 업데이트 → wallet_address 컬럼을 NULL로 초기화
pub async fn unlink_wallet(
    state: &AppState,
    user_id: Uuid,
    wallet_address: &str,
    nonce: &str,
    signature: &str,
) -> Result<()> {
    // 1) 연동 여부 확인
    //  public.users에서 이 user_id의 wallet_address가 NULL인지 확인
    //  wallet_address=is.null 필터: IS NULL 조건
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&wallet_address=not.is.null&select=id,wallet_address",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let resp = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("지갑 연동 여부 조회 HTTP 요청 실패")?;

    // PostgREST는 항상 배열로 응답: [] or [{"id": "..."}]
    let rows: Vec<serde_json::Value> =
        resp.json().await.context("지갑 연동 여부 JSON 파싱 실패")?;

    let linked_wallet_address = rows
        .first()
        .and_then(|row| row.get("wallet_address"))
        .and_then(|value| value.as_str())
        .ok_or_else(|| anyhow!("연동된 지갑이 없습니다."))?;

    if linked_wallet_address != wallet_address {
        return Err(anyhow!(
            "연동된 지갑 주소와 서명 지갑 주소가 일치하지 않습니다."
        ));
    }

    verify_wallet_nonce_and_signature(state, wallet_address, nonce, signature)?;

    // 2) DB 업데이트 (wallet_address → NULL)
    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let resp = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        // serde_json::Value::Null → PostgreSQL NULL로 변환됨
        .json(&serde_json::json!({"wallet_address": serde_json::Value::Null}))
        .send()
        .await
        .context("지갑 해제 DB 업데이트 HTTP 요청 실패")?;

    if !resp.status().is_success() {
        let err_text = resp.text().await.unwrap_or_default();
        return Err(anyhow!("지갑 해제 DB 업데이트 실패: {}", err_text));
    }

    tracing::info!("지갑 연동 해제 완료: user_id={}", user_id);

    Ok(())
}
