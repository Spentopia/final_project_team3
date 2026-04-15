// shared/lib/api.ts
// ─────────────────────────────────────────────────────────────
// 백엔드(Rust Axum) API를 호출할 때 사용하는 유틸 함수
//
// 왜 필요한가?
// 백엔드 보호 API를 호출하려면 매번 Authorization 헤더에 JWT를 넣어야 함.
// 매번 fetch할 때마다 헤더를 직접 쓰면 반복 코드가 많아지니까
// 여기서 자동으로 토큰을 붙여주는 wrapper를 만듦.
//
// 사용 예시:
//   import { apiFetch } from "@/shared/lib/api";
//
//   // GET 요청 (토큰 자동 첨부)
//   const data = await apiFetch("/me");
//
//   // POST 요청
//   const data = await apiFetch("/expenses", {
//     method: "POST",
//     body: JSON.stringify({ amount: 10000, category: "food" }),
//   });
import { authStorage } from "./auth";

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

export async function apiFetch(path: string, options: RequestInit = {}) {
    //localStorage에서 JWT 토큰을 가져옴
    const token = authStorage.getToken();

    //fetch 호출 시 자동으로 헤더에 토큰 첨부
    const res = await fetch('${BACKEND_URL}${path}', {
        ...options,
        headers: {
            "Content-Type": "application/json",
            //토큰이 있으면 Authorization 헤더에 넣고, 없으면 안 넣음
            ...(token ? {Authorization: 'Bearer ${token}'} : {}),
            //호출자가 추가 헤더를 넣었으면 그것도 합침
            ...options.headers,//
        },
    });

    if (!res.ok){
        const error = await res.text();
        throw new Error(error);
    }

    // 성공이면 JSON으로 파싱해서 반환
    return res.json();
}



