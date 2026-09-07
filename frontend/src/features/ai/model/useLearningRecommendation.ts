import { getGeneratedLearningRecommendation, requestLearningRecommendation } from "../api/generationApi";
import { useGeneration } from "./useGeneration";

export function useLearningRecommendation(learningRoadmapId: string) {
  return useGeneration(() => requestLearningRecommendation(learningRoadmapId), getGeneratedLearningRecommendation);
}
