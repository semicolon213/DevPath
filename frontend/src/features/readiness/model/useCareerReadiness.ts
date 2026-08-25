import { useQuery } from "@tanstack/react-query";
import { getCareerReadinessWorkspace, getCurrentCareerReadiness } from "../api/careerReadinessApi";

export const currentCareerReadinessKey = ["career-readiness", "current"] as const;

export function useCurrentCareerReadiness() {
  return useQuery({ queryKey: currentCareerReadinessKey, queryFn: getCurrentCareerReadiness });
}

export function useCareerReadinessWorkspace(careerReadinessId: string | undefined) {
  return useQuery({
    queryKey: ["career-readiness", careerReadinessId, "workspace"],
    queryFn: () => getCareerReadinessWorkspace(careerReadinessId!),
    enabled: Boolean(careerReadinessId)
  });
}
