import { apiRequest } from "../../../shared/api/apiClient";

export type CareerStage = "STUDENT" | "ENTRY_LEVEL" | "JUNIOR" | "MID_LEVEL" | "SENIOR";
export type UserProfile = { profileId: string; displayName: string; careerStage: CareerStage | null; bio: string | null; updatedAt: string };
export type UserPreferences = { careerId: string | null; companyId: string | null; updatedAt: string | null };

type CsrfToken = { headerName: string; token: string };

async function csrfInit(method: string, body: unknown): Promise<RequestInit> {
  const csrf = await apiRequest<CsrfToken>("/api/v1/csrf");
  return {
    method,
    headers: { "Content-Type": "application/json", [csrf.data.headerName]: csrf.data.token },
    body: JSON.stringify(body)
  };
}

export async function getProfile() { return (await apiRequest<UserProfile>("/api/v1/users/me/profile")).data; }
export async function updateProfile(profile: Pick<UserProfile, "displayName" | "careerStage" | "bio">) {
  return (await apiRequest<UserProfile>("/api/v1/users/me/profile", await csrfInit("PATCH", profile))).data;
}
export async function getPreferences() { return (await apiRequest<UserPreferences>("/api/v1/users/me/preferences")).data; }
export async function setCareer(careerId: string) {
  return (await apiRequest<UserPreferences>("/api/v1/users/me/preferences/career", await csrfInit("PUT", { careerId }))).data;
}
export async function setCompany(companyId: string) {
  return (await apiRequest<UserPreferences>("/api/v1/users/me/preferences/company", await csrfInit("PUT", { companyId }))).data;
}
