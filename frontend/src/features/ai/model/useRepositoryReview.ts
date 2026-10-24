import { getGeneratedRepositoryReview, requestRepositoryReview } from "../api/generationApi";
import { useGeneration } from "./useGeneration";

export function useRepositoryReview(analysisId: string) {
  return useGeneration(`REPOSITORY_REVIEW:${analysisId}`, () => requestRepositoryReview(analysisId), getGeneratedRepositoryReview);
}
