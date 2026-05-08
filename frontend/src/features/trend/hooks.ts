import { useQuery } from "@tanstack/react-query";
import { fetchTrends, type TrendBoardResponse } from "./api";

export function useTrends(limit = 50) {
  return useQuery<TrendBoardResponse>({
    queryKey: ["trends", limit],
    queryFn: () => fetchTrends(limit),
    staleTime: 5 * 60 * 1000,
  });
}
