import { expect, it } from "vitest";
import { readinessTargetLabel } from "./readinessTargetLabel";

it("shows a neutral label while the career catalog is loading", () => {
  expect(readinessTargetLabel({
    careers: undefined,
    careerId: "backend",
    profileVersion: "career-v2",
    pending: true,
    prefix: "목표"
  })).toBe("목표 확인 중");
});
