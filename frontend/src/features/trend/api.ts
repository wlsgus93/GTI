import { apiRequest } from "@/lib/api/client";

export type TrendBoardItem = {
  id: string;
  title: string;
  genre: string;
  platform: string;
  trendScore: number;
  ccuDeltaPct: number | null;
  concurrentPlayers: number;
};

export type TrendBoardResponse = {
  content: TrendBoardItem[];
  totalElements: number;
  requestedLimit: number;
};

export function fetchTrends(limit = 50): Promise<TrendBoardResponse> {
  return apiRequest<TrendBoardResponse>("/api/v1/trends", { query: { limit } });
}
