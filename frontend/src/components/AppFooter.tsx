import { Link } from "react-router";

export function AppFooter() {
  return (
    <footer className="border-t border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
      <div className="mx-auto flex max-w-[1400px] flex-col gap-4 px-4 py-8 text-sm text-zinc-600 md:flex-row md:items-center md:justify-between dark:text-zinc-400">
        <div>
          <p className="font-semibold text-zinc-900 dark:text-zinc-50">GameTrend-Insight</p>
          <p className="mt-1 text-xs">© {new Date().getFullYear()} 검은토끼흰토끼 · 프로토타입 UI</p>
        </div>
        <nav className="flex flex-wrap gap-x-6 gap-y-2" aria-label="푸터 링크 (목업)">
          <a href="#" className="hover:text-zinc-900 dark:hover:text-zinc-100">
            이용약관
          </a>
          <a href="#" className="hover:text-zinc-900 dark:hover:text-zinc-100">
            개인정보처리방침
          </a>
          <a href="#" className="hover:text-zinc-900 dark:hover:text-zinc-100">
            문의
          </a>
          <Link to="/discover" className="hover:text-zinc-900 dark:hover:text-zinc-100">
            데이터 출처
          </Link>
        </nav>
      </div>
    </footer>
  );
}
