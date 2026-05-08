export type MockPublisher = {
  id: string;
  name: string;
  gameCount: number;
  sharePct: number;
};

export const MOCK_PUBLISHERS: MockPublisher[] = [
  { id: "pub-valve", name: "Valve", gameCount: 42, sharePct: 8.2 },
  { id: "pub-from", name: "FromSoftware", gameCount: 12, sharePct: 3.1 },
  { id: "pub-cdp", name: "CD PROJEKT RED", gameCount: 8, sharePct: 2.4 },
];
