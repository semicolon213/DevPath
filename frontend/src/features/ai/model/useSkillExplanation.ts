import { getGeneratedSkillExplanation, requestSkillExplanation } from "../api/generationApi";
import { useGeneration } from "./useGeneration";

export function useSkillExplanation(skillMatrixId: string) {
  return useGeneration(() => requestSkillExplanation(skillMatrixId), getGeneratedSkillExplanation);
}
