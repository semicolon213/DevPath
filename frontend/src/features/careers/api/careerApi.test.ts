import { afterEach, expect, it, vi } from "vitest";
import { getCareer, getCareers } from "./careerApi";
afterEach(() => vi.unstubAllGlobals());
const metadata = { requestId: "r", apiVersion: "v1", timestamp: "2026-08-12T00:00:00Z" };
it("loads versioned supported career profiles", async () => {
  const fetchMock = vi.fn().mockResolvedValueOnce(Response.json({ data: { catalogVersion: "career-v1", careers: [{ careerId: "backend", localizedName: "백엔드 엔지니어" }] }, metadata })).mockResolvedValueOnce(Response.json({ data: { careerId: "backend", profileVersion: "career-v1", requiredCompetencies: ["테스트"] }, metadata }));
  vi.stubGlobal("fetch", fetchMock);
  await expect(getCareers()).resolves.toMatchObject({ catalogVersion: "career-v1" });
  await expect(getCareer("backend")).resolves.toMatchObject({ requiredCompetencies: ["테스트"] });
  expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/api/v1/careers/backend"), expect.anything());
});
