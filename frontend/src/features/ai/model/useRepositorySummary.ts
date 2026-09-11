import { getGeneratedRepositorySummary, requestRepositorySummary } from "../api/generationApi";
import { useGeneration } from "./useGeneration";

export function useRepositorySummary(analysisId: string) {
  return useGeneration(() => requestRepositorySummary(analysisId), getGeneratedRepositorySummary);
}
