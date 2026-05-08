/**
 * Iter 8 — Framer Motion 공통 variants & transitions.
 *
 * 원칙:
 * - duration 200~400ms (UX 방해 X)
 * - cubic-bezier(0.16, 1, 0.3, 1) — ease-out-expo (Apple/Linear 표준)
 * - prefers-reduced-motion 자동 존중 (Framer Motion 내장)
 */

import type { Transition, Variants } from "framer-motion";

/** 표준 ease — Linear/Vercel 톤 */
export const EASE_OUT_EXPO: Transition["ease"] = [0.16, 1, 0.3, 1];

/** Page transition — route 전환 */
export const pageTransition: Variants = {
  initial: { opacity: 0, y: 12 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -8 },
};

export const pageTransitionConfig: Transition = {
  duration: 0.32,
  ease: EASE_OUT_EXPO,
};

/** Staggered list — 트렌드 보드 / 게임 list 시간차 등장 */
export const staggerContainer: Variants = {
  hidden: {},
  visible: {
    transition: { staggerChildren: 0.06, delayChildren: 0.05 },
  },
};

export const staggerItem: Variants = {
  hidden: { opacity: 0, y: 16 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.36, ease: EASE_OUT_EXPO },
  },
};

/** Scroll-driven — 차트 / 큰 섹션 등장 */
export const scrollFadeUp: Variants = {
  hidden: { opacity: 0, y: 24 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.48, ease: EASE_OUT_EXPO },
  },
};

/** Card hover — 미세 lift */
export const cardHover = {
  rest: { y: 0 },
  hover: { y: -4, transition: { duration: 0.28, ease: EASE_OUT_EXPO } },
};

/** Chat bubble — AgentPanel 메시지 등장 */
export const chatBubbleVariants = (role: "user" | "assistant"): Variants => ({
  hidden: {
    opacity: 0,
    x: role === "user" ? 20 : -20,
    scale: 0.96,
  },
  visible: {
    opacity: 1,
    x: 0,
    scale: 1,
    transition: { duration: 0.28, ease: EASE_OUT_EXPO },
  },
});

/** Hero title kinetic — 글자 스플릿 시간차 */
export const kineticChar: Variants = {
  hidden: { opacity: 0, y: 18, filter: "blur(8px)" },
  visible: {
    opacity: 1,
    y: 0,
    filter: "blur(0)",
    transition: { duration: 0.4, ease: EASE_OUT_EXPO },
  },
};

export const kineticContainer: Variants = {
  hidden: {},
  visible: {
    transition: { staggerChildren: 0.04 },
  },
};
