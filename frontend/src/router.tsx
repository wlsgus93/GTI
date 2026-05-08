import { createBrowserRouter, Navigate } from "react-router";
import { MainLayout } from "@/layouts/MainLayout";
import { WorkspaceLayout } from "@/layouts/WorkspaceLayout";
import { AgentHomePage } from "@/pages/AgentHomePage";

/**
 * AgentHomePage 는 default route 라 첫 LCP 영향 — eager import.
 * 그 외 모든 페이지는 route-level lazy → 진입 시 fetch.
 *
 * 차트 의존성 (Recharts) 는 vite.config.ts manualChunks 로 별도 chunk.
 * → CCU/Compare/MoneyCalc 진입 시만 로드 (첫 LCP에 미포함).
 */
export const router = createBrowserRouter([
  {
    path: "/login",
    lazy: async () => {
      const m = await import("@/pages/LoginPlaceholderPage");
      return { Component: m.LoginPlaceholderPage };
    },
  },
  {
    path: "/signup",
    lazy: async () => {
      const m = await import("@/pages/SignupPlaceholderPage");
      return { Component: m.SignupPlaceholderPage };
    },
  },
  {
    path: "/",
    element: <MainLayout />,
    children: [
      { index: true, element: <AgentHomePage /> },
      { path: "agent", element: <Navigate to="/" replace /> },
      {
        path: "discover",
        lazy: async () => {
          const m = await import("@/pages/TrendBoardPage");
          return { Component: m.TrendBoardPage };
        },
      },
      { path: "trend", element: <Navigate to="/discover" replace /> },
      {
        path: "workspace",
        element: <WorkspaceLayout />,
        children: [
          { index: true, element: <Navigate to="compare" replace /> },
          {
            path: "compare",
            lazy: async () => {
              const m = await import("@/pages/ComparePage");
              return { Component: m.ComparePage };
            },
          },
          {
            path: "watchlist",
            lazy: async () => {
              const m = await import("@/pages/WatchlistPage");
              return { Component: m.WatchlistPage };
            },
          },
          {
            path: "money-calc",
            lazy: async () => {
              const m = await import("@/pages/MoneyCalcPage");
              return { Component: m.MoneyCalcPage };
            },
          },
        ],
      },
      {
        path: "cpv",
        lazy: async () => {
          const m = await import("@/pages/CpvPage");
          return { Component: m.CpvPage };
        },
      },
      {
        path: "games/:id",
        lazy: async () => {
          const m = await import("@/pages/GameDetailPage");
          return { Component: m.GameDetailPage };
        },
      },
      {
        path: "internal",
        lazy: async () => {
          const m = await import("@/pages/InternalReportPage");
          return { Component: m.InternalReportPage };
        },
      },
      {
        path: "verification",
        lazy: async () => {
          const m = await import("@/pages/VerificationPage");
          return { Component: m.VerificationPage };
        },
      },
      {
        path: "publishers",
        lazy: async () => {
          const m = await import("@/pages/PublisherListPage");
          return { Component: m.PublisherListPage };
        },
      },
      {
        path: "publishers/:id",
        lazy: async () => {
          const m = await import("@/pages/PublisherDetailPage");
          return { Component: m.PublisherDetailPage };
        },
      },
      {
        path: "campaigns",
        lazy: async () => {
          const m = await import("@/pages/CampaignsPage");
          return { Component: m.CampaignsPage };
        },
      },
      {
        path: "reports",
        lazy: async () => {
          const m = await import("@/pages/ReportsPage");
          return { Component: m.ReportsPage };
        },
      },
      { path: "compare", element: <Navigate to="/workspace/compare" replace /> },
      { path: "watchlist", element: <Navigate to="/workspace/watchlist" replace /> },
      { path: "money-calc", element: <Navigate to="/workspace/money-calc" replace /> },
    ],
  },
]);
