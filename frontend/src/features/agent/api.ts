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
};

export function postAgentQuery(req: AgentRequest): Promise<AgentResponse> {
  return apiRequest<AgentResponse>("/api/v1/agent/query", {
    method: "POST",
    body: req,
    requireAuth: true,
  });
}
