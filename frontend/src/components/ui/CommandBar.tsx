import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { usePersonaTheme } from "@/design/PersonaThemeContext";
import { ALL_PERSONAS, PERSONA_THEMES } from "@/design/personaThemes";

type Suggestion = {
  label: string;
  hint: string;
  run: () => void;
};

/**
 * CommandBar — 슬래시 명령 + 자연어 입력.
 *
 * - 룰 `15-agentic-ux.mdc` 의 "검색창 X — 에이전트 시작점" 정합
 * - `Cmd/Ctrl + K` 로 입력에 focus
 * - 슬래시 명령:
 *   `/discover` 트렌드 보드
 *   `/compare 730,1245620` 비교
 *   `/persona indie` 페르소나 전환
 *   `/game 730` 게임 상세
 *
 * 자연어 입력은 향후 LLM Query Planner 연결 — 현재는 슬래시 명령만 동작.
 */
export function CommandBar() {
  const inputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const { setPersona, theme } = usePersonaTheme();
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);

  // Cmd+K / Ctrl+K → focus
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  // query 변경 → 슬래시 자동완성
  useEffect(() => {
    const q = query.trim();
    if (!q) {
      setSuggestions([]);
      return;
    }
    const out: Suggestion[] = [];
    const lower = q.toLowerCase();
    if (lower.startsWith("/")) {
      // /discover
      if ("/discover".startsWith(lower) || lower.startsWith("/discover")) {
        out.push({ label: "/discover", hint: "트렌드 보드 (P1)", run: () => navigate("/discover") });
      }
      // /compare ids
      if (lower.startsWith("/compare")) {
        const m = lower.match(/^\/compare\s+([\d,\s]+)$/);
        const ids = m ? m[1].replace(/\s/g, "") : "730,1245620";
        out.push({
          label: `/compare ${ids}`,
          hint: "여러 게임 동시 비교 (Virtual Threads 병렬)",
          run: () => navigate(`/workspace/compare?ids=${ids}`),
        });
      } else if ("/compare".startsWith(lower)) {
        out.push({
          label: "/compare 730,1245620",
          hint: "여러 게임 동시 비교",
          run: () => navigate("/workspace/compare?ids=730,1245620"),
        });
      }
      // /game id
      if (lower.startsWith("/game")) {
        const m = lower.match(/^\/game\s+(\d+)$/);
        if (m) {
          out.push({
            label: `/game ${m[1]}`,
            hint: "게임 상세 (P2 6 탭)",
            run: () => navigate(`/games/${m[1]}`),
          });
        } else if ("/game".startsWith(lower)) {
          out.push({ label: "/game 730", hint: "게임 상세 (예: CS2 = 730)", run: () => navigate("/games/730") });
        }
      }
      // /persona
      if (lower.startsWith("/persona")) {
        const m = lower.match(/^\/persona\s+(\w+)$/i);
        const tail = m ? m[1].toUpperCase() : "";
        ALL_PERSONAS.forEach((p) => {
          if (!tail || p.startsWith(tail)) {
            out.push({
              label: `/persona ${p.toLowerCase()}`,
              hint: `${PERSONA_THEMES[p].label} 으로 전환`,
              run: () => setPersona(p),
            });
          }
        });
      } else if ("/persona".startsWith(lower)) {
        out.push({
          label: "/persona indie | publisher | marketer | investor",
          hint: "페르소나 전환",
          run: () => inputRef.current?.focus(),
        });
      }
      // /watchlist
      if ("/watchlist".startsWith(lower)) {
        out.push({
          label: "/watchlist",
          hint: "내 워치리스트 (로그인 필요)",
          run: () => navigate("/workspace/watchlist"),
        });
      }
      // /money
      if ("/money".startsWith(lower) || lower.startsWith("/money")) {
        out.push({
          label: "/money",
          hint: "MoneyCalc 의사결정 시뮬레이터 (P5)",
          run: () => navigate("/workspace/money-calc"),
        });
      }
      // /verify
      if ("/verify".startsWith(lower) || lower.startsWith("/verify")) {
        out.push({
          label: "/verify",
          hint: "Pretotyping 검증 모듈 (P7)",
          run: () => navigate("/verification"),
        });
      }
    } else {
      // 자연어 — TODO: LLM Query Planner 연동 (백엔드 endpoint 필요)
      out.push({
        label: `"${q}" — 자연어 검색`,
        hint: "(향후 LLM Query Planner 연결) 지금은 슬래시 명령만 동작",
        run: () => {},
      });
    }
    setSuggestions(out.slice(0, 5));
  }, [query, navigate, setPersona]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (suggestions.length > 0) {
      suggestions[0].run();
      setQuery("");
    }
  };

  return (
    <div className="space-y-2">
      <form onSubmit={handleSubmit} className="relative">
        <div className="flex items-center gap-2 rounded-[var(--radius-card)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] px-4 py-3 transition focus-within:border-[var(--color-accent)] focus-within:shadow-[0_0_0_3px_var(--color-accent-soft)]">
          <span aria-hidden className="text-[var(--color-ink-subtle)]">
            ⌘K
          </span>
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={`${theme.honorific}, 무엇이 궁금하세요? (/discover · /compare · /persona · /game)`}
            className="flex-1 bg-transparent text-[length:var(--text-body)] text-[var(--color-ink)] placeholder:text-[var(--color-ink-subtle)] focus:outline-none"
            aria-label="명령 또는 자연어 입력"
            autoComplete="off"
          />
          <button
            type="submit"
            className="shrink-0 rounded-[var(--radius-input)] bg-[var(--color-accent)] px-3 py-1 text-xs font-medium text-white transition hover:opacity-90"
          >
            실행
          </button>
        </div>
      </form>
      {suggestions.length > 0 ? (
        <ul className="overflow-hidden rounded-[var(--radius-card)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] shadow-[0_8px_32px_-12px_rgba(0,0,0,0.12)]">
          {suggestions.map((s, i) => (
            <li key={s.label}>
              <button
                type="button"
                onClick={() => {
                  s.run();
                  setQuery("");
                }}
                className="flex w-full items-center justify-between gap-3 px-4 py-2.5 text-left transition hover:bg-[var(--color-accent-soft)]"
              >
                <div className="min-w-0">
                  <p className="font-data text-sm text-[var(--color-ink)]">{s.label}</p>
                  <p className="text-xs text-[var(--color-ink-muted)]">{s.hint}</p>
                </div>
                {i === 0 ? (
                  <span className="shrink-0 rounded border border-[var(--color-line)] px-1.5 py-0.5 text-[10px] text-[var(--color-ink-muted)]">
                    Enter
                  </span>
                ) : null}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
