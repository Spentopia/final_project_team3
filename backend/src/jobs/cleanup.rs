// ─────────────────────────────────────────────────────────────
// 회원탈퇴 라이프사이클 정리 로직
// ─────────────────────────────────────────────────────────────
//
// Phase 2 (30일 후): 쿨다운 식별자 정리
//   - provider_id, login_provider, google_connected → null/false
//   - 같은 카카오/구글 계정으로 신규 가입 가능해짐
//   - users row 자체는 그대로 유지 (5년 보관)
//
// Phase 3 (5년 후): 완전 익명화
//   - 부가 데이터 hard delete (user_settings, gacha_logs 등)
//   - users row 컬럼 전부 익명화 (id만 유지)
//   - 거래/결제 이력은 user_id 외래키로만 유지
//   - anonymized_at 마커로 재처리 방지
//
// 법적 근거:
// - 전자상거래법 시행령 제6조: 결제/거래 기록 5년 보관
// - 전자금융거래법 시행령 제12조: 전자금융거래 기록 5년
// - 개인정보보호법 제21조: 보유 목적 달성 시 즉시 파기 (다른 법령 우선)

use anyhow::{Context, Result, anyhow};
use serde::Deserialize;
use serde_json::json;
use uuid::Uuid;

use crate::state::AppState;

/// 처리 대상 user 조회용
#[derive(Debug, Deserialize)]
struct WithdrawnUserRow {
    id: Uuid,
}

// ═══════════════════════════════════════════════════════════════
// Phase 2: 30일 지난 탈퇴자의 쿨다운 식별자 정리
// ═══════════════════════════════════════════════════════════════
//
// 처리 대상:
// - deleted_at IS NOT NULL
// - deleted_at <= NOW() - 30일
// - provider_id IS NOT NULL (이미 정리된 row는 제외)
//
// 처리 내용:
// - provider_id = NULL
// - login_provider = NULL
// - google_connected = false
//
// 효과:
// - 같은 카카오 계정으로 신규 가입 가능
// - 같은 구글 이메일로 신규 가입 가능
// - 단, 신규 가입이지 기존 계정 복구가 아님
//
// wallet_address는 처리 안 함:
// - withdraw_user에서 즉시 null로 처리되므로 불필요
pub async fn cleanup_30day_withdrawals(state: &AppState) -> Result<usize> {
    // 30일 전 시각 계산
    let cutoff = chrono::Utc::now() - chrono::Duration::days(30);
    let cutoff_str = cutoff.to_rfc3339();

    let url = format!(
        "{}/rest/v1/users?deleted_at=lte.{}&deleted_at=not.is.null&provider_id=not.is.null&select=id",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(&cutoff_str)
    );

    // PATCH 페이로드: 쿨다운 식별자 NULL/false 처리
    let payload = json!({
        "provider_id": null,
        "login_provider": null,
        "google_connected": false,
        "updated_at": chrono::Utc::now()
    });

    let resp = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        // return=representation으로 처리된 row 목록을 받아서 카운트
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .context("Phase 2 (30일 쿨다운 정리) PATCH 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("Phase 2 (30일 쿨다운 정리) PATCH 실패: {}", err));
    }

    let rows: Vec<WithdrawnUserRow> = resp.json().await.context("Phase 2 응답 파싱 실패")?;

    Ok(rows.len())
}

// ═══════════════════════════════════════════════════════════════
// Phase 3: 5년 지난 탈퇴자 완전 익명화
// ═══════════════════════════════════════════════════════════════
//
// 처리 대상:
// - deleted_at IS NOT NULL
// - deleted_at <= NOW() - 5년
// - anonymized_at IS NULL (아직 익명화 안 된 row만)
//
// 처리 내용 (유저별 순차 처리):
// 1) 부가 데이터 hard delete (user_settings, gacha_logs, ...)
// 2) public.users 컬럼 전부 익명화 (id, anonymized_at만 의미 있는 값)
//
// 거래/결제 데이터는 유지:
// - payments, market_transactions, expenses 등
// - user_id 외래키로 유지 → 본인 식별 불가 상태
//
// 트랜잭션:
// - Supabase REST API는 멀티 테이블 트랜잭션 직접 지원 X
// - 한 유저 처리 중 일부 실패해도 다른 유저는 계속 진행
// - 실패한 유저는 anonymized_at이 NULL로 남아 다음 배치에서 재시도됨
pub async fn cleanup_5year_withdrawals(state: &AppState) -> Result<usize> {
    // 5년 전 시각 계산 (윤년 무시 → 365 * 5)
    let cutoff = chrono::Utc::now() - chrono::Duration::days(365 * 5);
    let cutoff_str = cutoff.to_rfc3339();

    // 1) 익명화 대상 user_id 조회
    let select_url = format!(
        "{}/rest/v1/users?deleted_at=lte.{}&deleted_at=not.is.null&anonymized_at=is.null&select=id",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(&cutoff_str)
    );

    let resp = state
        .http_client
        .get(&select_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("Phase 3 (5년 익명화) 대상 조회 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("Phase 3 (5년 익명화) 대상 조회 실패: {}", err));
    }

    let users: Vec<WithdrawnUserRow> = resp.json().await.context("Phase 3 대상 응답 파싱 실패")?;

    if users.is_empty() {
        tracing::info!("Phase 3 (5년 익명화): 대상 없음");
        return Ok(0);
    }

    tracing::info!("Phase 3 (5년 익명화) 대상: {}건", users.len());

    let mut success_count = 0;

    // 2) 유저별 순차 익명화
    for user in users {
        match anonymize_user(state, user.id).await {
            Ok(_) => {
                success_count += 1;
                tracing::info!("5년 익명화 완료: user_id={}", user.id);
            }
            Err(e) => {
                tracing::error!(
                    "5년 익명화 실패 (다음 유저 계속): user_id={}, error={}",
                    user.id,
                    e
                );
                // 한 명 실패해도 다음 유저 계속 처리
                // anonymized_at이 NULL로 남아 다음 배치에서 재시도됨
            }
        }
    }

    Ok(success_count)
}

/// 개별 유저 익명화 (Phase 3의 핵심 로직)
///
/// 처리 순서:
/// 1) 부가 데이터 테이블에서 user_id로 검색되는 row hard delete
/// 2) public.users 컬럼 전부 익명화 + anonymized_at 마커
///
/// 실패 처리:
/// - 부가 데이터 삭제는 일부 실패해도 warn 로그 남기고 계속 진행
/// - public.users PATCH가 실패해야만 전체 함수 Err 반환
///   (anonymized_at이 안 찍히면 다음 배치에서 재시도)
async fn anonymize_user(state: &AppState, user_id: Uuid) -> Result<()> {
    let supabase = state.config.supabase_url.trim_end_matches('/');
    let secret = &state.config.supabase_secret_key;

    // ── 1) 부가 데이터 hard delete ─────────────────────────
    //
    // 개인 식별성이 강하지만 거래/결제와 무관한 데이터들.
    // 5년 보관 의무 대상이 아니라 즉시 삭제 가능.
    let tables_to_delete = vec![
        "user_settings",     // 알림 설정 등
        "streaks",           // 출석 스트릭
        "monthly_scores",    // 월간 성실도 점수
        "weekly_scores",     // 기존 주간 성실도 점수(전환 전 데이터)
        "notifications",     // 알림 이력
        "chatbot_logs",      // 챗봇 대화 기록
        "user_screenshots",  // 업로드 스크린샷
        "user_items",        // 아바타 아이템 보유
        "gacha_tickets",     // 가챠 티켓
        "gacha_logs",        // 가챠 결과 (개인 식별성)
        "budgets",           // 개인 예산 설정
        "budget_categories", // 예산 카테고리
        "fixed_expenses",    // 개인 고정비
        "reports",           // 개인 분석 리포트
    ];

    for table in tables_to_delete {
        let url = format!("{}/rest/v1/{}?user_id=eq.{}", supabase, table, user_id);

        let resp = state
            .http_client
            .delete(&url)
            .header("apikey", secret)
            .header("Authorization", format!("Bearer {}", secret))
            .header("Prefer", "return=minimal")
            .send()
            .await;

        match resp {
            Ok(r) if r.status().is_success() => {
                tracing::debug!("{} 삭제 성공: user_id={}", table, user_id);
            }
            Ok(r) => {
                let err = r.text().await.unwrap_or_default();
                tracing::warn!(
                    "{} 삭제 실패 (계속 진행): user_id={}, error={}",
                    table,
                    user_id,
                    err
                );
            }
            Err(e) => {
                tracing::warn!(
                    "{} 삭제 요청 실패 (계속 진행): user_id={}, error={}",
                    table,
                    user_id,
                    e
                );
            }
        }
    }

    // ── 2) public.users 익명화 ─────────────────────────────
    //
    // 거래 테이블이 user_id 외래키로 참조 중이라 row 삭제 불가.
    // 대신 모든 식별 가능 컬럼을 NULL/익명값으로 덮어씀.
    //
    // anonymized_at 마커:
    // - NOT NULL이 되면 다음 배치에서 다시 처리 안 됨
    // - 감사 로그 / 디버깅 용도
    let users_url = format!("{}/rest/v1/users?id=eq.{}", supabase, user_id);
    let now = chrono::Utc::now();

    let payload = json!({
        // 식별 정보 모두 익명화
        "email": format!("anonymized_{}@deleted.local", user_id),
        "phone": null,
        "nickname": format!("익명회원_{}", &user_id.to_string()[..8]),
        "profile_image": null,

        // 인증 식별자 모두 NULL
        "wallet_address": null,
        "provider_id": null,
        "login_provider": null,
        "google_connected": false,

        // 잔액/포인트 초기화
        "spt_balance": 0,

        // 익명화 마커
        "anonymized_at": now,
        "updated_at": now
    });

    let resp = state
        .http_client
        .patch(&users_url)
        .header("apikey", secret)
        .header("Authorization", format!("Bearer {}", secret))
        .header("Content-Type", "application/json")
        .header("Prefer", "return=minimal")
        .json(&payload)
        .send()
        .await
        .context("public.users 익명화 PATCH 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("public.users 익명화 실패: {}", err));
    }

    Ok(())
}
