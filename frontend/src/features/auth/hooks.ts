import { useMutation } from "@tanstack/react-query";
import { useAuth } from "@/auth/AuthContext";
import { loginUser, registerUser, type AuthResponse, type LoginPayload, type RegisterPayload } from "./api";

function toStored(res: AuthResponse) {
  return {
    token: res.token,
    userId: res.userId,
    email: res.email,
    displayName: res.displayName,
    role: res.role,
    expiresAt: Date.now() + res.expiresInSeconds * 1000,
  };
}

export function useLogin() {
  const { setAuth } = useAuth();
  return useMutation({
    mutationFn: (payload: LoginPayload) => loginUser(payload),
    onSuccess: (res) => setAuth(toStored(res)),
  });
}

export function useRegister() {
  const { setAuth } = useAuth();
  return useMutation({
    mutationFn: (payload: RegisterPayload) => registerUser(payload),
    onSuccess: (res) => setAuth(toStored(res)),
  });
}
