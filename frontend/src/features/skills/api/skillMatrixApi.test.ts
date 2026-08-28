import { afterEach, expect, it, vi } from "vitest";
import { getCurrentSkillMatrix, getSkillHistoryPage, getSkillMatrixComparison, getSkillWorkspace } from "./skillMatrixApi";

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

it("composes a skill history page from owner-scoped analysis history and immutable matrices", async () => {
  const assessment = { assessmentId: "assessment-1", skillId: "skill-1", skillKey: "testing-discipline",
    skillName: "Testing Discipline", category: "TESTING", score: 72, level: "COMPETENT", confidence: 90,
    strength: false, weakness: false, growthTrend: "UNAVAILABLE", aggregateRuleResultReference: "category:TESTING",
    evidenceIds: ["evidence-1"], repositoryIds: ["repository-1"], recommendationInputFacts: [], ruleSetVersion: "baseline-v2" };
  const fetchMock = vi.fn((input: string | URL | Request) => {
    const url = String(input);
    if (url.includes("/api/v1/analyses?")) return Promise.resolve(Response.json({ data: { analyses: [
      { analysisId: "analysis-1", repositoryId: "repository-1", repositoryFullName: "owner/devpath", skillMatrixId: "matrix-1" }
    ], nextCursor: "older", totalCount: 2 }, metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" } }));
    return Promise.resolve(Response.json({ data: { skillMatrixId: "matrix-1", evaluationId: "evaluation-1",
      policyVersion: "skill-matrix-v1", ruleSetVersion: "baseline-v2", status: "CURRENT",
      skills: [assessment], strengths: [], weaknesses: [], generatedAt: "2026-08-25T00:00:00Z" },
      metadata: { requestId: "r", apiVersion: "v1", timestamp: "2026-08-25T00:00:00Z" } }));
  });
  vi.stubGlobal("fetch", fetchMock);

  await expect(getSkillHistoryPage("skill-1")).resolves.toMatchObject({
    points: [{ analysisId: "analysis-1", skillMatrixId: "matrix-1", skill: { score: 72 } }],
    nextCursor: "older", totalAnalysisCount: 2
  });
  expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/analyses?limit=20",
    expect.objectContaining({ credentials: "include" }));
  expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/skill-matrices/matrix-1",
    expect.objectContaining({ credentials: "include" }));
});
