import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/auth/AuthContext";
import { addToWatchlist, fetchWatchlist, removeFromWatchlist, type AddWatchlistPayload } from "./api";

const QUERY_KEY = ["watchlist"] as const;

export function useWatchlist() {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: fetchWatchlist,
    enabled: isAuthenticated,
    staleTime: 60 * 1000,
  });
}

export function useAddWatchlist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: AddWatchlistPayload) => addToWatchlist(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  });
}

export function useRemoveWatchlist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (gameId: number) => removeFromWatchlist(gameId),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  });
}
