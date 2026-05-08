import { AnimatePresence, motion } from "framer-motion";
import { useState, useRef, useEffect, type FormEvent } from "react";
import { useAgentQuery } from "@/features/agent/hooks";
import type {
  AgentResponse,
  AgentTopic,
  AgentIntent,
} from "@/features/agent/api";
import { chatBubbleVariants, EASE_OUT_EXPO } from "@/design/motion";
import { loadAuth } from "@/lib/api/token";

type AgentPanelProps = {
  open: boolean;
  onClose: () => void;
};

type Turn = {
  role: "user" | "assistant";
  content: string;
  meta?: {
    topic: AgentTopic;
    intent: AgentIntent;
    classifierBlocked: boolean;
    model: string | null;
    tokens: number;
    latencyMs: number;
  };
};

const TOPIC_LABEL: Record<AgentTopic, string> = {
  GAME: "게임 분석",
  OFF_TOPIC: "범위 외",
  SMALL_TALK: "인사",
};

export function AgentPanel({ open, onClose }: AgentPanelProps) {
  const [draft, setDraft] = useState("");
  const [turns, setTurns] = useState<Turn[]>([]);
  const [sessionId, setSessionId] = useState<number | undefined>();
  const listRef = useRef<HTMLDivElement>(null);
  const isAuthed = Boolean(loadAuth());
  const { mutate, isPending } = useAgentQuery();

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [turns]);

  // AgentPanel — AnimatePresence 로 slide-in 처리
  if (!open) return null;

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const query = draft.trim();
    if (!query || isPending) return;

    setTurns((prev) => [...prev, { role: "user", content: query }]);
    setDraft("");

    mutate(
      { query, sessionId },
      {
        onSuccess: (res: AgentResponse) => {
          setSessionId(res.sessionId);
          setTurns((prev) => [
            ...prev,
            {
              role: "assistant",
              content: res.content,
              meta: {
                topic: res.topic,
                intent: res.intent,
                classifierBlocked: res.classifierBlocked,
                model: res.model,
                tokens: res.promptTokens + res.completionTokens,
                latencyMs: res.latencyMs,
              },
            },
          ]);
        },
        onError: (err) => {
          setTurns((prev) => [
            ...prev,
            {
              role: "assistant",
              content: `오류: ${err.message}`,
            },
          ]);
        },
      },
    );
  };

  return (
    <motion.aside
      initial={{ x: "100%", opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      exit={{ x: "100%", opacity: 0 }}
      transition={{ duration: 0.36, ease: EASE_OUT_EXPO }}
      className="glass-card fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col border-l border-white/10 shadow-2xl"
      aria-label="에이전트 패널"
    >
      <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3 dark:border-zinc-800">
        <div className="flex flex-col">
          <span className="font-semibold text-zinc-900 dark:text-zinc-50">
            Game-Agent
          </span>
          {sessionId !== undefined && (
            <span className="text-xs text-zinc-500">session #{sessionId}</span>
          )}
        </div>
        <button
          type="button"
          className="rounded-md px-2 py-1 text-sm text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-900"
          onClick={onClose}
        >
          닫기
        </button>
      </div>

      <div
        ref={listRef}
        className="flex flex-1 flex-col gap-3 overflow-y-auto p-4 text-sm"
      >
        {turns.length === 0 && (
          <p className="rounded-lg bg-zinc-100 px-3 py-2 text-zinc-600 dark:bg-zinc-900 dark:text-zinc-400">
            게임 시장에 대해 무엇이든 질문하세요.
            <br />
            예: &ldquo;Counter-Strike 2 의 최근 동향&rdquo;, &ldquo;인디 RPG
            출시 적기?&rdquo;
          </p>
        )}
        <AnimatePresence initial={false}>
        {turns.map((t, i) => (
          <motion.div
            key={i}
            variants={chatBubbleVariants(t.role)}
            initial="hidden"
            animate="visible"
            className={
              t.role === "user"
                ? "self-end max-w-[85%] rounded-lg bg-[var(--color-accent)] px-3 py-2 text-white shadow-md"
                : "self-start max-w-[90%] rounded-lg bg-zinc-100 px-3 py-2 text-zinc-900 dark:bg-zinc-900/80 dark:text-zinc-100"
            }
          >
            <div className="whitespace-pre-wrap">{t.content}</div>
            {t.meta && (
              <div className="mt-1 flex flex-wrap gap-1 text-[10px] text-zinc-500 dark:text-zinc-500">
                <span className="rounded bg-zinc-200 px-1 dark:bg-zinc-800">
                  {TOPIC_LABEL[t.meta.topic]}
                </span>
                {t.meta.classifierBlocked ? (
                  <span className="rounded bg-amber-200 px-1 text-amber-900 dark:bg-amber-900 dark:text-amber-200">
                    Layer 1 차단 (cloud 호출 X)
                  </span>
                ) : (
                  <>
                    <span className="rounded bg-zinc-200 px-1 dark:bg-zinc-800">
                      {t.meta.model ?? "—"}
                    </span>
                    <span className="rounded bg-zinc-200 px-1 dark:bg-zinc-800">
                      {t.meta.tokens} tok
                    </span>
                  </>
                )}
                <span className="rounded bg-zinc-200 px-1 dark:bg-zinc-800">
                  {t.meta.latencyMs}ms
                </span>
              </div>
            )}
          </motion.div>
        ))}
        </AnimatePresence>
        {isPending && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: [0.4, 1, 0.4] }}
            transition={{ duration: 1.4, repeat: Infinity, ease: "easeInOut" }}
            className="self-start rounded-lg bg-zinc-100 px-3 py-2 text-zinc-500 dark:bg-zinc-900"
          >
            응답 생성 중...
          </motion.div>
        )}
      </div>

      <form
        onSubmit={handleSubmit}
        className="flex gap-2 border-t border-zinc-200 p-3 dark:border-zinc-800"
      >
        <input
          type="text"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={isAuthed ? "질문 입력…" : "로그인 후 사용 가능"}
          className="flex-1 rounded-lg border border-zinc-300 bg-white px-3 py-2 text-zinc-900 disabled:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:disabled:bg-zinc-900"
          disabled={!isAuthed || isPending}
          aria-disabled={!isAuthed || isPending}
        />
        <button
          type="submit"
          className="btn-micro rounded-lg bg-[var(--color-accent)] px-4 py-2 font-medium text-white shadow-lg disabled:opacity-50"
          disabled={!isAuthed || isPending || !draft.trim()}
        >
          전송
        </button>
      </form>
    </motion.aside>
  );
}
