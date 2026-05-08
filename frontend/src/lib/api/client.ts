import { clearAuth, loadAuth } from "./token";
import { toApiError } from "./error";

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

type RequestOpts = {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  query?: Record<string, string | number | undefined | null>;
  signal?: AbortSignal;
  /** 인증 헤더를 강제로 부착 (Bearer 토큰 없으면 401 발생). */
  requireAuth?: boolean;
};

function buildUrl(path: string, query?: RequestOpts["query"]): string {
  const url = new URL((BASE_URL || window.location.origin) + path);
  if (query) {
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") {
        url.searchParams.append(k, String(v));
      }
    });
  }
  return url.toString();
}

/**
 * 공통 fetch wrapper.
 *
 * - JSON 직렬화 + Bearer 자동 부착
 * - 204 No Content는 undefined 반환
 * - 4xx/5xx는 ApiError 던짐 — TanStack Query가 errorBoundary로 수렴
 * - 401 시 저장된 토큰 자동 정리 (만료된 케이스)
 */
export async function apiRequest<T>(path: string, opts: RequestOpts = {}): Promise<T> {
  const auth = loadAuth();
  const headers: Record<string, string> = {
    Accept: "application/json",
  };
  if (opts.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (auth) {
    headers.Authorization = `Bearer ${auth.token}`;
  } else if (opts.requireAuth) {
    throw new Error("인증이 필요합니다 — 로그인 후 다시 시도하세요");
  }

  const response = await fetch(buildUrl(path, opts.query), {
    method: opts.method ?? "GET",
    headers,
    body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
    signal: opts.signal,
  });

  if (response.status === 401) {
    clearAuth();
    throw await toApiError(response);
  }
  if (!response.ok) {
    throw await toApiError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
