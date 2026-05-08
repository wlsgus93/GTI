import {
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Tooltip,
} from "recharts";

/**
 * 7차원 분석 시각화 — `docs/analysis-dimensions.md` (D1~D7).
 *
 * 현재 구현된 차원 = D2 (트렌드/흥행), D5 (커뮤니티) 등 일부.
 * 미구현 차원은 placeholder (0 또는 회색).
 *
 * 모든 점수는 0~100 정규화 (호출자 책임).
 */

export type DimensionAxis = {
  /** 'D1' ~ 'D7' */
  key: string;
  /** 한국어 한 줄 라벨 */
  label: string;
  /** 0~100 점수 — null = 미구현/데이터 부족 */
  score: number | null;
  /** placeholder 표시용 */
  implemented: boolean;
};

type Props = {
  axes: DimensionAxis[];
  /** 차트 높이 (px) */
  height?: number;
};

export function DimensionRadar({ axes, height = 300 }: Props) {
  // recharts 는 null → 0 으로 그리되, 미구현은 별도 series 로 회색 표시
  const data = axes.map((a) => ({
    label: a.label,
    key: a.key,
    score: a.score ?? 0,
    implemented: a.implemented ? a.score ?? 0 : 0,
    placeholder: a.implemented ? 0 : 100,
  }));

  return (
    <div style={{ width: "100%", height }}>
      <ResponsiveContainer width="100%" height="100%">
        <RadarChart data={data} margin={{ top: 12, right: 24, bottom: 12, left: 24 }}>
          <PolarGrid stroke="var(--color-line)" />
          <PolarAngleAxis
            dataKey="label"
            tick={{ fontSize: 11, fill: "var(--color-ink-muted)" }}
          />
          <PolarRadiusAxis
            domain={[0, 100]}
            tick={false}
            axisLine={false}
          />
          <Tooltip
            contentStyle={{
              background: "var(--color-surface-raised)",
              border: "1px solid var(--color-line)",
              borderRadius: "var(--radius-input)",
              fontSize: 12,
              color: "var(--color-ink)",
            }}
            formatter={(value, name) => {
              if (name === "placeholder") {
                return ["미구현 차원", ""];
              }
              return [`${Math.round(Number(value))}점`, "점수"];
            }}
            separator=""
          />
          {/* 미구현 차원 — 옅은 회색으로 채움 */}
          <Radar
            name="placeholder"
            dataKey="placeholder"
            stroke="transparent"
            fill="var(--color-line)"
            fillOpacity={0.15}
          />
          {/* 실제 점수 — 페르소나 accent */}
          <Radar
            name="score"
            dataKey="implemented"
            stroke="var(--color-accent)"
            strokeWidth={2}
            fill="var(--color-accent)"
            fillOpacity={0.18}
          />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
}
