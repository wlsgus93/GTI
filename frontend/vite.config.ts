import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    chunkSizeWarningLimit: 350,
    rollupOptions: {
      output: {
        manualChunks: {
          // 무거운 차트 라이브러리는 별도 chunk — 차트 페이지 진입 시만 로드
          recharts: ["recharts"],
          // 라우팅 — 첫 로드에 필요
          router: ["react-router"],
          // 서버 상태 — 첫 로드에 필요
          query: ["@tanstack/react-query"],
          // (react/react-dom 은 vite 가 자동으로 main chunk 에 묶음 — 명시 시 empty chunk)
        },
      },
    },
  },
});
