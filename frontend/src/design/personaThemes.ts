import type { Persona } from "@/features/insight/api";

/**
 * 4 페르소나 동등 시각 정체성.
 *
 * - 룰 `00-project-overview.mdc`: 4 페르소나 동등 시민
 * - 룰 `90-data-analyst-persona.mdc` §3: 페르소나별 톤·KPI·전략 형태
 * - PersonaThemeProvider 가 활성 페르소나의 토큰을 `--color-accent-*` 로 매핑
 */

export type PersonaTheme = {
  persona: Persona;
  /** 한국어 라벨 (UI 표시) */
  label: string;
  /** 인사 호칭 — Hero typography */
  honorific: string;
  /** 톤 한 줄 — Hero subtitle */
  tone: string;
  /** 시각 정체성 한 줄 — 디자인 의도 */
  visualHint: string;
  /** CSS var 베이스 (index.css 의 --color-{persona} 시리즈) */
  cssVar: string;
};

export const PERSONA_THEMES: Record<Persona, PersonaTheme> = {
  INDIE: {
    persona: "INDIE",
    label: "인디 개발자",
    honorific: "개발자님",
    tone: "친근하게 — 자본/시간 한계 안에서 어디 들어갈지 함께 고민",
    visualHint: "세이지 그린 — 조직적, 식물성, 친근",
    cssVar: "indie",
  },
  PUBLISHER: {
    persona: "PUBLISHER",
    label: "퍼블리셔·기획",
    honorific: "기획자님",
    tone: "전략적으로 — 포트폴리오 관점, IP·기술 적합성",
    visualHint: "슬레이트 바이올렛 — 위계, 포트폴리오, 전략",
    cssVar: "publisher",
  },
  MARKETER: {
    persona: "MARKETER",
    label: "마케터·UA",
    honorific: "마케터님",
    tone: "ROI 직답 — 채널 우선순위와 예산 배분",
    visualHint: "테라코타 — 행동 유도, 따뜻한 어반",
    cssVar: "marketer",
  },
  INVESTOR: {
    persona: "INVESTOR",
    label: "투자자",
    honorific: "투자자님",
    tone: "정밀하게 — 리스크 명시, profit prob, BEP",
    visualHint: "그래파이트 + 앰버 — financial terminal",
    cssVar: "investor",
  },
};

export const ALL_PERSONAS: Persona[] = ["INDIE", "PUBLISHER", "MARKETER", "INVESTOR"];

/**
 * 활성 페르소나의 token을 `--color-accent-*` 로 미러링.
 * style 객체로 반환 — 호출자가 root element 의 inline style에 적용.
 */
export function personaAccentVars(persona: Persona): Record<string, string> {
  const v = PERSONA_THEMES[persona].cssVar;
  return {
    "--color-accent": `var(--color-${v})`,
    "--color-accent-soft": `var(--color-${v}-soft)`,
    "--color-accent-strong": `var(--color-${v}-strong)`,
  };
}
