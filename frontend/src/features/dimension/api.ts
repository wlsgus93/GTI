import { apiRequest } from "@/lib/api/client";

export type GenreStats = {
  genre: string;
  gameCount: number;
  avgLatestCcu: number | null;
  maxLatestCcu: number | null;
  topGameName: string | null;
  hitCount: number;
  flopCount: number;
  normalCount: number;
};

export type YearStats = {
  year: number;
  gameCount: number;
  avgLatestCcu: number | null;
};

export type ReleaseDimension = {
  byGenre: GenreStats[];
  byYear: YearStats[];
  totalGames: number;
  generatedAt: string;
};

export type SnapshotSource = "STEAM" | "STEAM_SPY" | "STEAM_STORE" | "TWITCH" | "YOUTUBE" | "REDDIT" | "OPENCRITIC" | "IGDB" | "APPLE";

export type ActivityClass = "VERY_ACTIVE" | "ACTIVE" | "NORMAL" | "QUIET" | "VERY_QUIET";

export type SentimentBreakdown = {
  positive: number;
  neutral: number;
  negative: number;
  positiveRatio: number | null;
};

export type PainPoint = {
  topic: string;
  description: string;
  mentionCount: number;
  sentiment: "POSITIVE" | "NEUTRAL" | "NEGATIVE";
};

export type CommunityDimension = {
  gameId: number;
  gameName: string;
  totalMentions: number;
  mentionsByPlatform: Partial<Record<SnapshotSource, number>>;
  sentiment: SentimentBreakdown;
  activityZScore: number | null;
  activityClass: ActivityClass;
  painPoints: PainPoint[];
  confidence: "HIGH" | "MEDIUM" | "LOW" | null;
  generatedAt: string;
};

export function fetchReleaseDimension(): Promise<ReleaseDimension> {
  return apiRequest<ReleaseDimension>("/api/v1/dimensions/d1-release");
}

export function fetchCommunityDimension(gameId: string | number): Promise<CommunityDimension> {
  return apiRequest<CommunityDimension>(`/api/v1/dimensions/d5-community/${gameId}`);
}
