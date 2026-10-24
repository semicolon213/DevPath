import { getGeneratedRepositorySummary, requestRepositorySummary } from "../api/generationApi";
import { useGeneration } from "./useGeneration";

export function useRepositorySummary(analysisId: string) {
  return useGeneration(`REPOSITORY_SUMMARY:${analysisId}`, () => requestRepositorySummary(analysisId), getGeneratedRepositorySummary);
}
