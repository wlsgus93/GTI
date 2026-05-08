import { apiRequest } from "@/lib/api/client";

export type Persona = "INDIE" | "PUBLISHER" | "MARKETER" | "INVESTOR";

export const PERSONA_LABEL: Record<Persona, string> = {
  INDIE: "인디 개발자",
  PUBLISHER: "퍼블리셔/기획",
  MARKETER: "마케터/UA",
  INVESTOR: "투자자",
};

export type GameInsight = {
  gameId: number;
  summary: string;
  promptVersion: string;
  totalTokens: number;
  model: string;
  cached: boolean;
  stale: boolean;
  generatedAt: string;
  expiresAt: string;
};

export type Perspective = {
  persona: Persona;
  personaLabel: string;
  summary: string;
  totalTokens: number;
  model: string;
  promptVersion: string;
  cached: boolean;
  stale: boolean;
  generatedAt: string;
  expiresAt: string;
};

export type MultiPersonaInsight = {
  gameId: number;
  perspectives: Perspective[];
  totalLatencyMs: number;
  respondedAt: string;
};

export function fetchInsight(id: string | number, persona: Persona = "INDIE"): Promise<GameInsight> {
  return apiRequest<GameInsight>(`/api/v1/games/${id}/insight`, { query: { persona } });
}

export function fetchMultiInsight(
  id: string | number,
  personas: Persona[],
): Promise<MultiPersonaInsight> {
  return apiRequest<MultiPersonaInsight>(`/api/v1/games/${id}/insights`, {
    query: { personas: personas.join(",") },
  });
}
