import { apiRequest } from "@/lib/api/client";

export type Scenario = {
  owners: number;
  priceCents: number;
};

export type MoneyCalcRequest = {
  pessimistic: Scenario;
  realistic: Scenario;
  optimistic: Scenario;
  developmentCostCents: number;
  marketingCostCents: number;
  monteCarloIterations?: number;
};

export type ScenarioOutput = {
  grossRevenueUsd: string;
  afterRefundUsd: string;
  developerNetUsd: string;
  totalCostUsd: string;
  profitUsd: string;
  roiPct: number | null;
  bepUnits: number | null;
};

export type MonteCarloOutput = {
  iterations: number;
  netRevenueP10: string;
  netRevenueP50: string;
  netRevenueP90: string;
  netRevenueMean: string;
  netRevenueStdDev: string;
  profitProbabilityPct: number | null;
};

export type SensitivityImpact = {
  variable: string;
  pessimisticNetUsd: string;
  optimisticNetUsd: string;
  impactRatio: number | null;
};

export type Assumptions = {
  refundRate: number;
  steamCut: number;
  monteCarloDistribution: number;
  randomSeed: number;
};

export type MoneyCalcResult = {
  pessimistic: ScenarioOutput;
  realistic: ScenarioOutput;
  optimistic: ScenarioOutput;
  monteCarlo: MonteCarloOutput;
  sensitivity: SensitivityImpact[];
  assumptions: Assumptions;
  generatedAt: string;
};

export function simulateMoneyCalc(payload: MoneyCalcRequest): Promise<MoneyCalcResult> {
  return apiRequest<MoneyCalcResult>("/api/v1/moneycalc/simulate", { method: "POST", body: payload });
}
