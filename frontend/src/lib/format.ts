/** 표시 포맷 유틸 — 한국식 천단위 콤마 + 압축 표기. */

const NF = new Intl.NumberFormat("ko-KR");

export function fmtInt(value: number | null | undefined, fallback = "—"): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return fallback;
  }
  return NF.format(value);
}

export function fmtCompact(value: number | null | undefined, fallback = "—"): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return fallback;
  }
  if (Math.abs(value) >= 1_000_000_000) {
    return (value / 1_000_000_000).toFixed(1) + "B";
  }
  if (Math.abs(value) >= 1_000_000) {
    return (value / 1_000_000).toFixed(1) + "M";
  }
  if (Math.abs(value) >= 1_000) {
    return (value / 1_000).toFixed(1) + "K";
  }
  return String(value);
}

export function fmtPct(value: number | null | undefined, decimals = 1): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "—";
  }
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(decimals)}%`;
}

export function fmtUsd(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "—";
  }
  return `$${fmtCompact(value)}`;
}

export function fmtRelative(iso: string | null | undefined): string {
  if (!iso) {
    return "—";
  }
  const ts = new Date(iso).getTime();
  if (Number.isNaN(ts)) {
    return "—";
  }
  const diffSec = Math.round((Date.now() - ts) / 1000);
  if (diffSec < 60) {
    return `${diffSec}초 전`;
  }
  if (diffSec < 3600) {
    return `${Math.round(diffSec / 60)}분 전`;
  }
  if (diffSec < 86400) {
    return `${Math.round(diffSec / 3600)}시간 전`;
  }
  return `${Math.round(diffSec / 86400)}일 전`;
}
