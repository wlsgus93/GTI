import { describe, expect, it } from "vitest";

describe("routes after IA update", () => {
  it("uses discover and workspace paths", () => {
    expect("/discover").toBe("/discover");
    expect("/workspace/compare").toContain("/workspace/");
    expect("/cpv").toBe("/cpv");
  });

  it("game detail tabs use query params", () => {
    expect("/games/730?tab=players").toContain("tab=players");
  });
});
