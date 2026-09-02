import { getGeneratedCareerAdvice, requestCareerCoaching } from "../api/generationApi";
import { useGeneration } from "./useGeneration";

export function useCareerCoaching(careerReadinessId: string) {
  return useGeneration(() => requestCareerCoaching(careerReadinessId), getGeneratedCareerAdvice);
}
