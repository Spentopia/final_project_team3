// domains/auth/hooks/useAuth.ts
// 로그인 상태를 감지하고 관리하는 커스텀 훅
// Supabase의 onAuthStateChange로 세션 변경을 실시간 감지

import {useEffect, useState} from "react";
import {supabase} from "@/shared/lib/supabase";
