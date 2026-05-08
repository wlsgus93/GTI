import { useQuery } from "@tanstack/react-query";
import { fetchCommunityDimension, fetchReleaseDimension } from "./api";

export function useReleaseDimension() {
  return useQuery({
    queryKey: ["dimension", "release"],
    queryFn: fetchReleaseDimension,
    staleTime: 30 * 60 * 1000,
  });
}

export function useCommunityDimension(gameId: string | number | undefined) {
  return useQuery({
    queryKey: ["dimension", "community", gameId],
    queryFn: () => fetchCommunityDimension(gameId!),
    enabled: gameId !== undefined,
    staleTime: 30 * 60 * 1000,
  });
}
