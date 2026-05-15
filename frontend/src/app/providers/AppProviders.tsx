import type { PropsWithChildren } from "react";
import { ThemeProvider } from "@/shared/providers/ThemeProvider";
import { SolanaWalletProvider } from "@/domains/wallet/providers/SolanaWalletProvider";
import { UserProvider } from "@/shared/context/UserContext";

export default function AppProviders({ children }: PropsWithChildren) {
  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
      <SolanaWalletProvider>
        <UserProvider>{children}</UserProvider>
      </SolanaWalletProvider>
    </ThemeProvider>
  );
}
