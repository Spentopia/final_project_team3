// shared/lib/supabase.ts
// ─────────────────────────────────────────────────────────────
// Supabase 클라이언트를 한 번만 생성해서 앱 전체에서 공유하는 파일
//
// 왜 하나만 만드나?
// Supabase 클라이언트는 내부적으로 세션 관리, 토큰 갱신 등을 해줌.
// 여러 개 만들면 세션이 꼬일 수 있어서 싱글톤으로 하나만 만들어야 함.
// 백엔드의 state.rs에서 AppState 하나 만들어서 공유하는 것과 같은 개념.
//
// 환경변수:
// VITE_SUPABASE_URL           — Supabase 프로젝트 URL
// VITE_SUPABASE_PUBLISHABLE_KEY — Supabase anon public 키
// Vite는 VITE_ 접두사가 붙은 환경변수만 프론트에서 접근 가능.
// .env 파일에 넣어두면 import.meta.env로 읽을 수 있음.

import {createClient} from "@supabase/supabase-js"

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY;

export const supabase = createClient(supabaseUrl, supabaseKey);