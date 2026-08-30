import { apiRequest, withCsrf } from "../../../shared/api/apiClient";

export type CareerStage = "STUDENT" | "ENTRY_LEVEL" | "JUNIOR" | "MID_LEVEL" | "SENIOR";
export type UserProfile = { profileId: string; displayName: string; careerStage: CareerStage | null; bio: string | null; updatedAt: string };
export type UserPreferences = { careerId: string | null; companyId: string | null; updatedAt: string | null };

async function csrfInit(method: string, body: unknown): Promise<RequestInit> {
  return withCsrf({
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
}

export async function getProfile() { return apiRequest<UserProfile>("/api/v1/users/me/profile"); }
export async function updateProfile(profile: Pick<UserProfile, "displayName" | "careerStage" | "bio">) {
  return apiRequest<UserProfile>("/api/v1/users/me/profile", await csrfInit("PATCH", profile));
}
export async function getPreferences() { return apiRequest<UserPreferences>("/api/v1/users/me/preferences"); }
export async function setCareer(careerId: string) {
  return apiRequest<UserPreferences>("/api/v1/users/me/preferences/career", await csrfInit("PUT", { careerId }));
}
export async function setCompany(companyId: string) {
  return apiRequest<UserPreferences>("/api/v1/users/me/preferences/company", await csrfInit("PUT", { companyId }));
}
