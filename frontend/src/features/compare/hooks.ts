import { useQuery } from "@tanstack/react-query";
import { fetchCompare } from "./api";

export function useCompare(ids: (string | number)[]) {
  return useQuery({
    queryKey: ["compare", ids.join(",")],
    queryFn: () => fetchCompare(ids),
    enabled: ids.length >= 2,
    staleTime: 60 * 1000,
  });
}
