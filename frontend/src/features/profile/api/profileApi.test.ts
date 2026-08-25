import { afterEach, describe, expect, it, vi } from "vitest";
import { getProfile, setCareer, updateProfile } from "./profileApi";

const metadata = { requestId: "request-1", apiVersion: "v1", timestamp: "2026-08-11T00:00:00Z" };

describe("profile API", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("retrieves the authenticated user's profile", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({ data: {
      profileId: "e046a279-9c82-4bbf-9d8f-0737b222fa97", displayName: "DevPath User",
      careerStage: "JUNIOR", bio: null, updatedAt: "2026-08-11T00:00:00Z"
    }, metadata })));
    await expect(getProfile()).resolves.toMatchObject({ displayName: "DevPath User", careerStage: "JUNIOR" });
  });

  it("uses the server CSRF token for profile and career mutations", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "token" }, metadata }))
      .mockResolvedValueOnce(Response.json({ data: { profileId: "e046a279-9c82-4bbf-9d8f-0737b222fa97", displayName: "Updated", careerStage: null, bio: null, updatedAt: metadata.timestamp }, metadata }))
      .mockResolvedValueOnce(Response.json({ data: { headerName: "X-CSRF-TOKEN", token: "token" }, metadata }))
      .mockResolvedValueOnce(Response.json({ data: { careerId: "backend", companyId: null, updatedAt: metadata.timestamp }, metadata }));
    vi.stubGlobal("fetch", fetchMock);

    await updateProfile({ displayName: "Updated", careerStage: null, bio: null });
    await setCareer("backend");

    expect(fetchMock).toHaveBeenNthCalledWith(2, expect.stringContaining("/profile"), expect.objectContaining({
      method: "PATCH", credentials: "include", headers: expect.objectContaining({ "X-CSRF-TOKEN": "token" })
    }));
    expect(fetchMock).toHaveBeenNthCalledWith(4, expect.stringContaining("/preferences/career"), expect.objectContaining({ method: "PUT" }));
  });
});
