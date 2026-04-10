import { Navigate } from "react-router";
import { authStorage } from "@/shared/lib/auth";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  if (!authStorage.isLoggedIn()) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}