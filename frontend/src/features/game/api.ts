import { apiRequest } from "@/lib/api/client";

export type GameDetail = {
  id: number;
  steamAppId: number | null;
  igdbId: number | null;
  name: string;
  description: string | null;
  developer: string | null;
  publisher: string | null;
  releaseDate: string | null;
  coverImageUrl: string | null;
  genres: string[];
  latestCcu: number | null;
  ccuDeltaPct: number | null;
  createdAt: string;
  updatedAt: string;
};

export type CcuPoint = { capturedAt: string; concurrentPlayers: number };
export type CcuSeries = { gameId: number; range: string; from: string; to: string; points: CcuPoint[] };

export type PlayerInsight = {
  gameId: number;
  players: {
    concurrentPlayers: number | null;
    reviewScorePositive: number | null;
    reviewScoreTotal: number | null;
    reviewScorePercent: number | null;
  };
  twitchViewers: number | null;
  mentions: { source: string; mentionCount: number; capturedAt: string }[];
  lastUpdated: string | null;
};

export function fetchGameDetail(id: string | number): Promise<GameDetail> {
  return apiRequest<GameDetail>(`/api/v1/games/${id}`);
}

export function fetchCcuSeries(id: string | number, range = "30d"): Promise<CcuSeries> {
  return apiRequest<CcuSeries>(`/api/v1/games/${id}/ccu`, { query: { range } });
}

export function fetchPlayerInsight(id: string | number): Promise<PlayerInsight> {
  return apiRequest<PlayerInsight>(`/api/v1/games/${id}/players`);
}
