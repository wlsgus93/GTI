import { apiRequest } from "@/lib/api/client";

export type WatchlistItem = {
  id: number;
  gameId: number;
  steamAppId: number | null;
  name: string;
  coverImageUrl: string | null;
  latestCcu: number | null;
  note: string | null;
  addedAt: string;
};

export type AddWatchlistPayload = {
  gameId: number;
  note?: string;
};

export function fetchWatchlist(): Promise<WatchlistItem[]> {
  return apiRequest<WatchlistItem[]>("/api/v1/watchlist", { requireAuth: true });
}

export function addToWatchlist(payload: AddWatchlistPayload): Promise<WatchlistItem> {
  return apiRequest<WatchlistItem>("/api/v1/watchlist", {
    method: "POST",
    body: payload,
    requireAuth: true,
  });
}

export function removeFromWatchlist(gameId: number): Promise<void> {
  return apiRequest<void>(`/api/v1/watchlist/${gameId}`, { method: "DELETE", requireAuth: true });
}
