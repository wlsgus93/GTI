import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { ErrorBox } from "@/components/AsyncState";
import { Card } from "@/components/ui/Card";
import { useLogin } from "@/features/auth/hooks";

export function LoginPlaceholderPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const login = useLogin();
  const from = (location.state as { from?: string } | null)?.from ?? "/";

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    login.mutate(
      { email, password },
      { onSuccess: () => navigate(from, { replace: true }) },
    );
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-[var(--color-surface)] px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-[var(--radius-card)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] p-8"
      >
        <Link
          to="/"
          className="text-[var(--text-meta)] font-bold tracking-tight text-[var(--color-ink)]"
        >
          GTI
        </Link>
        <h1 className="mt-3 text-xl font-bold text-[var(--color-ink)]">로그인</h1>
        <p className="mt-1 text-sm text-[var(--color-ink-muted)]">
          JWT Bearer — 모든 API 호출에 자동 부착
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
          <span className="text-[var(--color-ink)]">비밀번호</span>
          <input
            type="password"
            value={password}
            required
            minLength={8}
            autoComplete="current-password"
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 w-full rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-ink)]"
          />
        </label>

        {login.isError ? (
          <div className="mt-3">
            <ErrorBox error={login.error} />
          </div>
        ) : null}

        <button
          type="submit"
          disabled={login.isPending}
          className="mt-5 w-full rounded-[var(--radius-input)] bg-[var(--color-accent)] py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {login.isPending ? "로그인 중…" : "로그인"}
        </button>

        <div className="mt-4 flex items-center justify-between text-xs">
          <Link to="/signup" className="text-[var(--color-ink-muted)] underline">
            계정 만들기
          </Link>
          <Link to="/" className="font-medium text-[var(--color-accent-strong)] underline">
            홈으로
          </Link>
        </div>
      </form>

      <Card variant="sunken" className="mt-4 max-w-sm">
        <p className="text-[var(--text-meta)] text-[var(--color-ink-muted)]">
          GTI 는 4 페르소나 (인디·퍼블리셔·마케터·투자자) 동등 지원. 가입 후 페르소나 선택은 헤더의
          PersonaSwitcher 또는 CommandBar 의 <span className="font-data">/persona</span> 명령으로
          전환 가능.
        </p>
      </Card>
    </div>
  );
}
