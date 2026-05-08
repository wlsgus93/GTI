import { apiRequest } from "@/lib/api/client";

export type CompareItem = {
  gameId: number;
  steamAppId: number | null;
  name: string;
  genres: string[] | null;
  coverImageUrl: string | null;
  latestCcu: number | null;
  ccuDeltaPct: number | null;
  twitchViewers: number | null;
  totalMentions: number | null;
  reviewScorePercent: number | null;
  ownersMid: number | null;
  priceUsd: string | null;
  developerNetRevenue: string | null;
  confidence: string | null;
};

export type CompareResult = {
  items: CompareItem[];
  missingGameIds: number[];
  wallClockMs: number;
  generatedAt: string;
};

export function fetchCompare(ids: (string | number)[]): Promise<CompareResult> {
  return apiRequest<CompareResult>(`/api/v1/games/compare`, { query: { ids: ids.join(",") } });
}
