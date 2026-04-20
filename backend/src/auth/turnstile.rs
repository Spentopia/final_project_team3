// auth/turnstile.rs
//
// Cloudflare Turnstile 서버 검증 전용 파일
//
// 역할:
// - 프론트가 보낸 captcha_token을 Cloudflare Siteverify API로 검증
// - 성공하면 Ok(())
// - 실패하면 Err(...) 반환
//
// 왜 서버 검증이 필요하나?
// - 프론트에 위젯만 띄우는 건 보안이 아님
// - 실제로는 서버가 token을 검증해야 사람 인증이 완료된 것으로 본다

use std::time::Duration;
use anyhow::{anyhow, Context, Result};
use serde::Deserialize;

#[derive(Debug, Deserialize)]
struct TurnstileVerifyResponse {
    success: bool,

    #[serde(default)]
    error_codes: Vec<String>,
}

pub async fn verify_turnstile(
    http_client: &reqwest::Client,
    secret_key: &str,
    token: &str,
) -> Result<()> {
    let form = [
        ("secret", secret_key.to_string()),
        ("response", token.to_string()),
    ];

    let resp = http_client
        .post("https://challenges.cloudflare.com/turnstile/v0/siteverify")
        .timeout(Duration::from_secs(3))
        .form(&form)
        .send()
        .await
        .context("Turnstile 검증 요청 실패")?
        .error_for_status()
        .context("Turnstile 검증 응답 실패")?;

    let body: TurnstileVerifyResponse = resp
        .json()
        .await
        .context("Turnstile 검증 응답 JSON 파싱 실패")?;

    if !body.success {
        tracing::warn!("Turnstile 검증 실패: {:?}", body.error_codes);
        return Err(anyhow!("사람 인증 검증에 실패했습니다."));
    }

    Ok(())
}