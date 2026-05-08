import { useQuery } from "@tanstack/react-query";
import { fetchCampaignImpact, fetchCase, fetchCases } from "./api";

export function useCases() {
  return useQuery({
    queryKey: ["verification", "cases"],
    queryFn: fetchCases,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCaseDetail(code: string | undefined) {
  return useQuery({
    queryKey: ["verification", "case", code],
    queryFn: () => fetchCase(code!),
    enabled: code !== undefined,
    staleTime: 60 * 1000,
  });
}

export function useCampaignImpact(
  campaignId: number | undefined,
  gameId: number | undefined,
  maxLag = 14,
  enabled = false,
) {
  return useQuery({
    queryKey: ["verification", "campaignImpact", campaignId, gameId, maxLag],
    queryFn: () => fetchCampaignImpact(campaignId!, gameId!, maxLag),
    enabled: enabled && campaignId !== undefined && gameId !== undefined,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
}
