import { apiRequest } from "@/lib/api/client";
import type { Persona } from "@/features/insight/api";

export type AgentTopic = "GAME" | "OFF_TOPIC" | "SMALL_TALK";
export type AgentIntent =
  | "NEW_QUERY"
  | "FOLLOW_UP"
  | "PERSONA_SWITCH"
  | "META"
  | "UNCLEAR";

export type AgentRequest = {
  query: string;
  sessionId?: number;
  persona?: Persona;
};

export type AgentResponse = {
  sessionId: number;
  messageId: number;
  content: string;
  topic: AgentTopic;
  intent: AgentIntent;
  classifierBlocked: boolean;
  model: string | null;
  promptTokens: number;
  completionTokens: number;
  cached: boolean;
  latencyMs: number;
  /** W9 옵션 C — Agentic UX. 응답 시점의 chat_session.persona (자동 추론 결과) */
  activePersona: Persona | null;
  /** true = 이번 query 에서 페르소나 자동 추론됨 (UI 가 mesh morph 트리거) */
  personaInferred: boolean;
};

export function postAgentQuery(req: AgentRequest): Promise<AgentResponse> {
  return apiRequest<AgentResponse>("/api/v1/agent/query", {
    method: "POST",
    body: req,
    requireAuth: true,
  });
}
