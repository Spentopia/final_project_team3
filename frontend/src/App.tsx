import { RouterProvider } from "react-router";
import { router } from "./routes";
import { Toaster } from "./components/ui/sonner";
import { ThemeProvider } from "./providers/ThemeProvider";
import SolanaWalletProvider from "./providers/SolanaWalletProvider";

export default function App() {
  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
      <SolanaWalletProvider>
        <RouterProvider router={router} />
        <Toaster />
      </SolanaWalletProvider>
    </ThemeProvider>
  );
}
