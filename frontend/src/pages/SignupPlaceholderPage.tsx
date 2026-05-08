import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { ErrorBox } from "@/components/AsyncState";
import { Card } from "@/components/ui/Card";
import { useRegister } from "@/features/auth/hooks";

/**
 * W9 옵션 C — Agentic UX:
 * 페르소나 카드 4개 강제 선택 제거. 가입 후 onboarding 대화에서 시스템이 자동 추론.
 * 가입은 이메일/비밀번호/표시이름 만 — 최소 정보.
 */
export function SignupPlaceholderPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const register = useRegister();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    register.mutate(
      { email, password, displayName },
      // W9 옵션 C — 가입 직후 onboarding 대화로 (페르소나 자동 추론 시작점)
      { onSuccess: () => navigate("/onboarding", { replace: true }) },
    );
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[var(--color-surface)] px-4 py-10">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-md rounded-[var(--radius-card)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] p-8"
      >
        <Link
          to="/"
          className="text-[var(--text-meta)] font-bold tracking-tight text-[var(--color-ink)]"
        >
          GTI
        </Link>
        <h1 className="mt-3 text-xl font-bold text-[var(--color-ink)]">회원가입</h1>
        <p className="mt-1 text-sm text-[var(--color-ink-muted)]">
          가입 즉시 자동 로그인 — 에이전트가 첫 대화로 당신의 관점을 자동 파악합니다
        </p>

        <label className="mt-5 block text-sm">
          <span className="text-[var(--color-ink)]">이메일</span>
          <input
            type="email"
            value={email}
            required
            autoComplete="email"
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 w-full rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-ink)]"
          />
        </label>
        <label className="mt-3 block text-sm">
          <span className="text-[var(--color-ink)]">비밀번호 (8자 이상)</span>
          <input
            type="password"
            value={password}
            required
            minLength={8}
            autoComplete="new-password"
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 w-full rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-ink)]"
          />
        </label>
        <label className="mt-3 block text-sm">
          <span className="text-[var(--color-ink)]">표시 이름</span>
          <input
            type="text"
            value={displayName}
            required
            maxLength={100}
            onChange={(e) => setDisplayName(e.target.value)}
            className="mt-1 w-full rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-ink)]"
          />
        </label>

        {register.isError ? (
          <div className="mt-3">
            <ErrorBox error={register.error} />
          </div>
        ) : null}

        <button
          type="submit"
          disabled={register.isPending}
          className="btn-micro mt-5 w-full rounded-[var(--radius-input)] bg-[var(--color-accent)] py-2 text-sm font-medium text-white shadow-md disabled:opacity-50"
        >
          {register.isPending ? "가입 중…" : "회원가입"}
        </button>

        <div className="mt-4 flex items-center justify-between text-xs">
          <Link to="/login" className="text-[var(--color-ink-muted)] underline">
            이미 계정이 있나요?
          </Link>
          <Link to="/" className="font-medium text-[var(--color-accent-strong)] underline">
            홈으로
          </Link>
        </div>
      </form>

      <Card variant="sunken" className="max-w-md">
        <p className="text-[var(--text-meta)] text-[var(--color-ink-muted)]">
          가입 후 에이전트가 짧은 대화로 당신의 관점을 자동 파악합니다.
          <br />
          관점 (인디·퍼블리셔·마케터·투자자) 은 언제든 헤더 뱃지로 명시 변경 가능.
        </p>
      </Card>
    </div>
  );
}
