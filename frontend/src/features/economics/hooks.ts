import { useQuery } from "@tanstack/react-query";
import { fetchEconomics } from "./api";

export function useEconomics(id: string | number | undefined) {
  return useQuery({
    queryKey: ["economics", id],
    queryFn: () => fetchEconomics(id!),
    enabled: id !== undefined,
    staleTime: 60 * 60 * 1000,
  });
}
