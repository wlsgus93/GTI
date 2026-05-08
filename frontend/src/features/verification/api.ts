import { apiRequest } from "@/lib/api/client";

export type CaseStatus = "PLANNING" | "RUNNING" | "ANALYZING" | "DONE";

export type CaseSummary = {
  id: number;
  code: string;
  title: string;
  status: CaseStatus;
  priority: boolean;
  stimulusCount: number;
  campaignCount: number;
};

export type StimulusItem = {
  id: number;
  type: string;
  title: string;
  url: string;
  description: string | null;
  createdAt: string;
};

export type CampaignWithMetrics = {
  id: number;
  stimulusId: number | null;
  platform: string;
  name: string;
  utmCampaign: string;
  status: string;
  startedAt: string | null;
  endedAt: string | null;
  budgetCents: number;
  spentCents: number;
  totalImpressions: number;
  totalClicks: number;
  totalConversions: number;
  ctr: number | null;
  cvr: number | null;
  cpmCents: number | null;
  cpcCents: number | null;
};

export type CaseDetail = {
  id: number;
  code: string;
  title: string;
  concept: string | null;
  hypothesis: string | null;
  targetPersona: string | null;
  status: CaseStatus;
  priority: boolean;
  createdAt: string;
  updatedAt: string;
  stimuli: StimulusItem[];
  campaigns: CampaignWithMetrics[];
};

export function fetchCases(): Promise<CaseSummary[]> {
  return apiRequest<CaseSummary[]>("/api/v1/verification/cases");
}

export function fetchCase(code: string): Promise<CaseDetail> {
  return apiRequest<CaseDetail>(`/api/v1/verification/cases/${code}`);
}

export type CampaignImpactAnalysis = {
  campaignId: number;
  gameId: number;
  campaignName: string;
  bestLagDays: number | null;
  bestCorrelation: number | null;
  correlationsByLag: Record<string, number>;
  sampleSize: number;
  fromDate: string | null;
  toDate: string | null;
  interpretation: string;
  confidence: "HIGH" | "MEDIUM" | "LOW" | null;
  analyzedAt: string;
};

export function fetchCampaignImpact(
  campaignId: number,
  gameId: number,
  maxLag = 14,
): Promise<CampaignImpactAnalysis> {
  return apiRequest<CampaignImpactAnalysis>(
    `/api/v1/verification/campaigns/${campaignId}/impact`,
    { query: { gameId, maxLag } },
  );
}
