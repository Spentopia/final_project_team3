// shared/lib/supabase.ts
// Supabase 클라이언트를 한 번만 생성해서 앱 전체에서 공유
// 백엔드의 state.rs에서 AppState 하나 만들어서 공유하는 것과 같은 개념

import {createClient} from "@supabase/supabase-js"

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY;

export const supabase = createClient(supabaseUrl, supabaseKey);