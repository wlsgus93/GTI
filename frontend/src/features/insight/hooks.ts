import { useQuery } from "@tanstack/react-query";
import { fetchInsight, fetchMultiInsight, type Persona } from "./api";

export function useInsight(id: string | number | undefined, persona: Persona = "INDIE") {
  return useQuery({
    queryKey: ["insight", id, persona],
    queryFn: () => fetchInsight(id!, persona),
    enabled: id !== undefined,
    staleTime: 60 * 60 * 1000,
    retry: false,
  });
}

export function useMultiInsight(id: string | number | undefined, personas: Persona[]) {
  return useQuery({
    queryKey: ["insight", "multi", id, personas.join(",")],
    queryFn: () => fetchMultiInsight(id!, personas),
    enabled: id !== undefined && personas.length > 0,
    staleTime: 60 * 60 * 1000,
    retry: false,
  });
}
