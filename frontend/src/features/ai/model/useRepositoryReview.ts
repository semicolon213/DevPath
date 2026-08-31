import { getGeneratedRepositoryReview, requestRepositoryReview } from "../api/generationApi";
import { useGeneration } from "./useGeneration";

export function useRepositoryReview(analysisId: string) {
  return useGeneration(() => requestRepositoryReview(analysisId), getGeneratedRepositoryReview);
}
