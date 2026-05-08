import { useQuery } from "@tanstack/react-query";
import { fetchCcuSeries, fetchGameDetail, fetchPlayerInsight } from "./api";

export function useGameDetail(id: string | number | undefined) {
  return useQuery({
    queryKey: ["game", "detail", id],
    queryFn: () => fetchGameDetail(id!),
    enabled: id !== undefined,
    staleTime: 60 * 1000,
  });
}

export function useCcuSeries(id: string | number | undefined, range = "30d") {
  return useQuery({
    queryKey: ["game", "ccu", id, range],
    queryFn: () => fetchCcuSeries(id!, range),
    enabled: id !== undefined,
    staleTime: 60 * 1000,
  });
}

export function usePlayerInsight(id: string | number | undefined) {
  return useQuery({
    queryKey: ["game", "players", id],
    queryFn: () => fetchPlayerInsight(id!),
    enabled: id !== undefined,
    staleTime: 60 * 1000,
  });
}
