use axum::http::Request;

use std::net::{
    IpAddr,
    Ipv4Addr,
};

use tower_governor::{
    errors::GovernorError,
    key_extractor::KeyExtractor,
};

use uuid::Uuid;

/// Cloudflare + Railway 환경 전용 IP extractor.
///
/// 우선순위:
///
/// 1. CF-Connecting-IP
/// 2. X-Real-IP
/// 3. localhost fallback (개발 전용)
///
/// X-Forwarded-For는 spoof 가능성이 있으므로 사용하지 않는다.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct CloudflareRailwayIpExtractor;

impl KeyExtractor for CloudflareRailwayIpExtractor {
    type Key = IpAddr;

    fn extract<T>(
        &self,
        req: &Request<T>,
    ) -> Result<Self::Key, GovernorError> {
        let headers = req.headers();

        // ─────────────────────────────────────────────
        // 1. CF-Connecting-IP
        // ─────────────────────────────────────────────
        if let Some(ip) = headers
            .get("cf-connecting-ip")
            .and_then(|v| v.to_str().ok())
            .and_then(parse_ip)
        {
            return Ok(ip);
        }

        // ─────────────────────────────────────────────
        // 2. X-Real-IP
        // ─────────────────────────────────────────────
        if let Some(ip) = headers
            .get("x-real-ip")
            .and_then(|v| v.to_str().ok())
            .and_then(parse_ip)
        {
            return Ok(ip);
        }

        // ─────────────────────────────────────────────
        // 3. localhost fallback
        //
        // main.rs 수정 없이 로컬 테스트 가능하게 하기 위한 fallback.
        //
        // 운영 환경에서는:
        // - CF-Connecting-IP
        // - X-Real-IP
        //
        // 둘 중 하나가 항상 존재해야 정상.
        //
        // localhost 개발 환경에서는 헤더가 없으므로
        // 127.0.0.1을 사용한다.
        // ─────────────────────────────────────────────
        #[cfg(debug_assertions)]
        {
            return Ok(IpAddr::V4(Ipv4Addr::LOCALHOST));
        }

        Err(GovernorError::UnableToExtractKey)
    }
}

/// 문자열 → IpAddr 파싱
fn parse_ip(raw: &str) -> Option<IpAddr> {
    raw.trim().parse::<IpAddr>().ok()
}

// ═══════════════════════════════════════════════════════
// UserIdExtractor
// ═══════════════════════════════════════════════════════

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct UserIdExtractor;

impl KeyExtractor for UserIdExtractor {
    type Key = Uuid;

    fn extract<T>(
        &self,
        req: &Request<T>,
    ) -> Result<Self::Key, GovernorError> {
        req.extensions()
            .get::<Uuid>()
            .copied()
            .ok_or(GovernorError::UnableToExtractKey)
    }
}