import { createContext, useContext, useState, useCallback, type ReactNode } from "react";
import { getMe, type MeResponse } from "@/domains/auth/api/auth";

type UserContextValue = {
  user: MeResponse | null;
  setUser: (user: MeResponse | null) => void;
  refetchUser: () => Promise<void>;
};

const UserContext = createContext<UserContextValue | null>(null);

export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);

  const refetchUser = useCallback(async () => {
    try {
      const fresh = await getMe();
      setUser(fresh);
    } catch (e) {
      console.warn("/me 재조회 실패:", e);
    }
  }, []);

  return (
    <UserContext.Provider value={{ user, setUser, refetchUser }}>
      {children}
    </UserContext.Provider>
  );
}

export function useUser() {
  const ctx = useContext(UserContext);
  if (!ctx) throw new Error("useUser must be used within UserProvider");
  return ctx;
}
