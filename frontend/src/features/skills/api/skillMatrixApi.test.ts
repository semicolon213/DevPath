import { afterEach, expect, it, vi } from "vitest";
import { getCurrentSkillMatrix } from "./skillMatrixApi";

afterEach(() => vi.unstubAllGlobals());

it("requests the owner-scoped current Skill Matrix with credentials", async () => {
  const fetchMock = vi.fn().mockResolvedValue(Response.json({
    data: { skillMatrixId: "matrix-1", skills: [] },
    metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" }
  }));
  vi.stubGlobal("fetch", fetchMock);

  await getCurrentSkillMatrix();

  expect(fetchMock).toHaveBeenCalledWith(
    "http://localhost:8080/api/v1/skill-matrices/current",
    expect.objectContaining({ credentials: "include" })
  );
});
