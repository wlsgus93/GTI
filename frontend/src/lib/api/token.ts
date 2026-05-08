/** JWT 영속화 — localStorage. SSR 없어 단순. */

const STORAGE_KEY = "gti.token";

export type StoredAuth = {
  token: string;
  userId: number;
  email: string;
  displayName: string;
  role: string;
  expiresAt: number;
};

export function loadAuth(): StoredAuth | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as StoredAuth;
    if (Date.now() >= parsed.expiresAt) {
      window.localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function saveAuth(auth: StoredAuth): void {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
}

export function clearAuth(): void {
  window.localStorage.removeItem(STORAGE_KEY);
}
