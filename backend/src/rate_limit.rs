// src/rate_limit.rs
//
// Railway + Cloudflare Proxied 환경에서 rate limit 기준 IP를 안전하게 뽑기 위한 코드.
//
// 현재 배포 구조:
//
//   사용자 브라우저
//        ↓
//   Cloudflare Proxied DNS
//        ↓
//   Railway Proxy
//        ↓
//   Axum Backend
//
// 테스트 결과:
//
// 1) 일반 요청
//    cf-connecting-ip = 실제 사용자 IP
//    x-real-ip        = 실제 사용자 IP
//    peer_addr        = Railway 내부 IP, 예: 100.64.x.x
//
// 2) 위조 요청
//    curl -H "X-Forwarded-For: 8.8.8.8" ...
//
//    x-forwarded-for leftmost = 8.8.8.8        ← 공격자가 위조한 값
//    cf-connecting-ip         = 실제 사용자 IP  ← Cloudflare가 유지한 값
//    x-real-ip                = 실제 사용자 IP
//
// 결론:
// - peer_addr 기반 rate limit은 Railway 내부 IP 기준이라 부정확할 수 있음.
// - SmartIpKeyExtractor는 x-forwarded-for를 먼저 보기 때문에 위조값에 속을 수 있음.
// - 따라서 CF-Connecting-IP를 1순위로 사용한다.
// - CF-Connecting-IP가 없으면 X-Real-IP를 2순위로 사용한다.
// - X-Forwarded-For는 일부러 사용하지 않는다.

use axum::http::Request;
use std::net::IpAddr;
use tower_governor::{
    errors::GovernorError,
    key_extractor::KeyExtractor,
};

/// Cloudflare + Railway 환경 전용 IP 추출기.
///
/// tower_governor의 GovernorLayer는 요청마다 "key"를 하나 뽑아서
/// 그 key별로 token bucket을 관리한다.
///
/// 여기서 key는 사용자 IP다.
///
/// 예:
/// - A 사용자 IP = 1.1.1.1 → 1.1.1.1 버킷 사용
/// - B 사용자 IP = 2.2.2.2 → 2.2.2.2 버킷 사용
///
/// 이렇게 해야 사용자별 rate limit이 된다.
///
/// 반대로 peer_addr를 쓰면 Railway 내부 IP가 key가 될 수 있어서
/// 여러 사용자가 한 버킷에 묶이거나,
/// Cloudflare/Railway 프록시 구조 때문에 의도와 다르게 동작할 수 있다.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct CloudflareRailwayIpExtractor;

impl KeyExtractor for CloudflareRailwayIpExtractor {
    type Key = IpAddr;

    /// 요청에서 rate limit key로 사용할 IP를 추출한다.
    ///
    /// 네 tower_governor 버전에서는 KeyExtractor trait에
    /// name(), key_name() 메서드가 없으므로 구현하지 않는다.
    ///
    /// 필요한 건 extract() 하나다.
    fn extract<T>(&self, req: &Request<T>) -> Result<Self::Key, GovernorError> {
        let headers = req.headers();

        // ─────────────────────────────────────────────────────
        // 1순위: CF-Connecting-IP
        //
        // Cloudflare가 원본 클라이언트 IP를 담아주는 헤더.
        //
        // 네 테스트 결과:
        // - 일반 요청에서도 실제 IP
        // - X-Forwarded-For 위조 요청에서도 실제 IP 유지
        //
        // 따라서 Cloudflare Proxied 환경에서는 이 값을 가장 신뢰한다.
        // ─────────────────────────────────────────────────────
        if let Some(ip) = headers
            .get("cf-connecting-ip")
            .and_then(|value| value.to_str().ok())
            .and_then(parse_ip)
        {
            return Ok(ip);
        }

        // ─────────────────────────────────────────────────────
        // 2순위: X-Real-IP
        //
        // 네 테스트에서 X-Real-IP도 실제 사용자 IP로 들어왔다.
        // CF-Connecting-IP가 없는 특수 상황의 fallback으로만 사용한다.
        //
        // 단, Railway 기본 도메인으로 Cloudflare를 우회해서 직접 접근할 수 있다면
        // 클라이언트가 X-Real-IP를 위조할 가능성이 생긴다.
        //
        // 그래서 운영에서는 반드시 api.spentopia.net처럼
        // Cloudflare Proxied 도메인으로만 접근하게 하는 것이 좋다.
        // ─────────────────────────────────────────────────────
        if let Some(ip) = headers
            .get("x-real-ip")
            .and_then(|value| value.to_str().ok())
            .and_then(parse_ip)
        {
            return Ok(ip);
        }

        // ─────────────────────────────────────────────────────
        // X-Forwarded-For는 사용하지 않는다.
        //
        // 이유:
        // 네 위조 테스트에서 다음처럼 나왔다.
        //
        //   x_forwarded_for_raw:
        //   "8.8.8.8,182.220.224.48, 172.71.210.168"
        //
        // 여기서:
        // - leftmost = 8.8.8.8       → 공격자가 위조한 값
        // - middle   = 실제 사용자 IP
        // - rightmost= Cloudflare IP
        //
        // leftmost를 쓰면 공격자가 IP를 바꿔가며 rate limit을 우회할 수 있고,
        // rightmost를 쓰면 Cloudflare 엣지 IP로 묶여 사용자별 제한이 안 된다.
        // ─────────────────────────────────────────────────────

        Err(GovernorError::UnableToExtractKey)
    }
}

/// 헤더 문자열을 IpAddr로 파싱한다.
///
/// trim()을 하는 이유:
/// 헤더 값에 공백이 섞일 수 있기 때문.
///
/// 예:
/// - "182.220.224.48"
/// - " 182.220.224.48 "
fn parse_ip(raw: &str) -> Option<IpAddr> {
    raw.trim().parse::<IpAddr>().ok()
}