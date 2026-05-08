import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { ErrorBox } from "@/components/AsyncState";
import { PersonaSwitcher } from "@/components/ui/PersonaSwitcher";
import { Card } from "@/components/ui/Card";
import { useRegister } from "@/features/auth/hooks";

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
      { onSuccess: () => navigate("/", { replace: true }) },
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
          가입 즉시 JWT 발급 — 자동 로그인
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

        <div className="mt-5 border-t border-[var(--color-line)] pt-4">
          <p className="text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
            페르소나 선택 (지금 또는 가입 후 변경 가능)
          </p>
          <p className="mt-1 text-xs text-[var(--color-ink-subtle)]">
            모든 페이지·LLM 응답이 페르소나 톤·우선 KPI에 맞춰 분기됩니다.
          </p>
          <div className="mt-3">
            <PersonaSwitcher variant="detail" />
          </div>
        </div>

        {register.isError ? (
          <div className="mt-3">
            <ErrorBox error={register.error} />
          </div>
        ) : null}

        <button
          type="submit"
          disabled={register.isPending}
          className="mt-5 w-full rounded-[var(--radius-input)] bg-[var(--color-accent)] py-2 text-sm font-medium text-white disabled:opacity-50"
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
          GTI 는 게임 산업 종사자 4 페르소나 (인디·퍼블리셔·마케터·투자자) 모두를 동등 지원합니다.
          페르소나 전환은 언제든 가능 — 헤더 PersonaSwitcher 또는 CommandBar (Cmd+K) 의{" "}
          <span className="font-data">/persona</span> 명령.
        </p>
      </Card>
    </div>
  );
}
