import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { Persona } from "@/features/insight/api";
import { PERSONA_THEMES, personaAccentVars, type PersonaTheme } from "./personaThemes";

/**
 * 활성 페르소나 ↔ 시각 토큰 동기화.
 *
 * - default = `INDIE` (룰 00-project-overview.mdc — 시스템 fallback)
 * - localStorage 영속화 (`gti.persona`)
 * - <html> 의 inline style 에 `--color-accent-*` 자동 적용
 *   → 모든 자식 element 가 var(--color-accent) 로 페르소나 색 사용
 */

const STORAGE_KEY = "gti.persona";

type PersonaThemeContextValue = {
  persona: Persona;
  theme: PersonaTheme;
  setPersona: (next: Persona) => void;
};

const PersonaThemeContext = createContext<PersonaThemeContextValue | null>(null);

function loadStored(): Persona {
  if (typeof window === "undefined") {
    return "INDIE";
  }
  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (raw === "INDIE" || raw === "PUBLISHER" || raw === "MARKETER" || raw === "INVESTOR") {
    return raw;
  }
  return "INDIE";
}

export function PersonaThemeProvider({ children }: { children: ReactNode }) {
  const [persona, setPersonaState] = useState<Persona>(() => loadStored());

  const setPersona = useCallback((next: Persona) => {
    window.localStorage.setItem(STORAGE_KEY, next);
    setPersonaState(next);
  }, []);

  // <html> inline style 에 --color-accent-* 적용 (전역 영향)
  useEffect(() => {
    const root = document.documentElement;
    const vars = personaAccentVars(persona);
    Object.entries(vars).forEach(([k, v]) => {
      root.style.setProperty(k, v);
    });
    root.dataset.persona = persona.toLowerCase();
    return () => {
      // unmount 시 굳이 정리 X — 다른 persona 세팅이 덮어씀
    };
  }, [persona]);

  const value = useMemo<PersonaThemeContextValue>(
    () => ({ persona, theme: PERSONA_THEMES[persona], setPersona }),
    [persona, setPersona],
  );

  return <PersonaThemeContext.Provider value={value}>{children}</PersonaThemeContext.Provider>;
}

export function usePersonaTheme(): PersonaThemeContextValue {
  const ctx = useContext(PersonaThemeContext);
  if (!ctx) {
    throw new Error("usePersonaTheme must be used inside PersonaThemeProvider");
  }
  return ctx;
}
