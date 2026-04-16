import type { PropsWithChildren } from "react";
import { ThemeProvider } from "@/shared/providers/ThemeProvider";
import { SolanaWalletProvider } from "@/domains/wallet/providers/SolanaWalletProvider";

export default function AppProviders({ children }: PropsWithChildren) {
  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
      <SolanaWalletProvider>{children}</SolanaWalletProvider>
    </ThemeProvider>
  );
}
