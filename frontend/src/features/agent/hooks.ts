import { useMutation } from "@tanstack/react-query";
import { usePersonaTheme } from "@/design/PersonaThemeContext";
import { postAgentQuery, type AgentRequest, type AgentResponse } from "./api";

/**
 * Agent query mutation.
 *
 * <p>W9 옵션 C — 응답에 activePersona 가 있고 현재 활성 persona 와 다르면
 * PersonaThemeContext 자동 갱신 → 전 페이지 mesh 색상 / 차트 색상 morph.
 */
export function useAgentQuery() {
  const { persona: currentPersona, setPersona } = usePersonaTheme();

  return useMutation<AgentResponse, Error, AgentRequest>({
    mutationFn: postAgentQuery,
    onSuccess: (res) => {
      // 백엔드가 추론한 persona 가 현재와 다르면 전역 동기화
      if (res.activePersona && res.activePersona !== currentPersona) {
        setPersona(res.activePersona);
      }
    },
  });
}
