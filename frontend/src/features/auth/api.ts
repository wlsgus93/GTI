import { apiRequest } from "@/lib/api/client";

export type AuthResponse = {
  token: string;
  userId: number;
  email: string;
  displayName: string;
  role: string;
  expiresInSeconds: number;
};

export type RegisterPayload = {
  email: string;
  password: string;
  displayName: string;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export function registerUser(payload: RegisterPayload): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/v1/auth/register", { method: "POST", body: payload });
}

export function loginUser(payload: LoginPayload): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/v1/auth/login", { method: "POST", body: payload });
}
