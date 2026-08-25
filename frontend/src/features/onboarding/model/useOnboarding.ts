import { useQuery } from "@tanstack/react-query";
import { getOnboardingProgress } from "../api/onboardingApi";

export const onboardingProgressKey = ["onboarding", "progress"] as const;
export function useOnboardingProgress() { return useQuery({ queryKey: onboardingProgressKey, queryFn: getOnboardingProgress }); }
