import { useQuery } from "@tanstack/react-query";
import { getCurrentCareerReadiness } from "../api/careerReadinessApi";

export const currentCareerReadinessKey = ["career-readiness", "current"] as const;

export function useCurrentCareerReadiness() {
  return useQuery({ queryKey: currentCareerReadinessKey, queryFn: getCurrentCareerReadiness });
}
