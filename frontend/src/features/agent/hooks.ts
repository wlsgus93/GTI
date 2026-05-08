import { useMutation } from "@tanstack/react-query";
import { postAgentQuery, type AgentRequest, type AgentResponse } from "./api";

export function useAgentQuery() {
  return useMutation<AgentResponse, Error, AgentRequest>({
    mutationFn: postAgentQuery,
  });
}
