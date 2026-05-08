import { apiRequest } from "@/lib/api/client";

export type EconomicsInsight = {
  gameId: number;
  revenue: {
    ownersLow: number | null;
    ownersHigh: number | null;
    ownersMid: number | null;
    priceUsd: string | null;
    grossLifetimeRevenue: string | null;
    afterRefundRevenue: string | null;
    developerNet: string | null;
    estimatedDau: number | null;
    estimatedMau: number | null;
  } | null;
  unitEconomics: {
    viewToPlayRatio: number | null;
    mentionToPlayRatio: number | null;
    priceEfficiency: number | null;
    reviewCostPerPositive: string | null;
  } | null;
  confidence: "HIGH" | "MEDIUM" | "LOW" | null;
  lastUpdated: string | null;
};

export function fetchEconomics(id: string | number): Promise<EconomicsInsight> {
  return apiRequest<EconomicsInsight>(`/api/v1/games/${id}/economics`);
}
