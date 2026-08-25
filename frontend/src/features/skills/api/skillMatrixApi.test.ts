import { afterEach, expect, it, vi } from "vitest";
import { getCurrentSkillMatrix, getSkillMatrixComparison, getSkillWorkspace } from "./skillMatrixApi";

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

it("loads owner-scoped skill detail and evidence through separate canonical endpoints", async () => {
  const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(Response.json({
    data: {}, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" }
  })));
  vi.stubGlobal("fetch", fetchMock);

  await getSkillWorkspace("skill-1");

  expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/skills/skill-1",
    expect.objectContaining({ credentials: "include" }));
  expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/skills/skill-1/evidence",
    expect.objectContaining({ credentials: "include" }));
});

it("sends two matrix IDs as repeated comparison query parameters", async () => {
  const fetchMock = vi.fn().mockResolvedValue(Response.json({
    data: { matrices: [] }, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" }
  }));
  vi.stubGlobal("fetch", fetchMock);

  await getSkillMatrixComparison(["matrix-a", "matrix-b"]);

  expect(fetchMock).toHaveBeenCalledWith(
    "http://localhost:8080/api/v1/skill-matrices/compare?skillMatrixId=matrix-a&skillMatrixId=matrix-b",
    expect.objectContaining({ credentials: "include" })
  );
});
