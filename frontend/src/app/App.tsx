import { RouterProvider } from "react-router";
import { router } from "@/app/router/routes";
import AppProviders from "@/app/providers/AppProviders";
import { Toaster } from "@/shared/ui/sonner";
import { FinanceProvider } from "@/shared/providers/FinanceProvider";

export default function App() {
  return (
    <AppProviders>
        <RouterProvider router={router} />
        <Toaster />
    </AppProviders>
  );
}