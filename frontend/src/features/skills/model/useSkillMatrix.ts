import { useQuery } from "@tanstack/react-query";
import { getCurrentSkillMatrix } from "../api/skillMatrixApi";

export const currentSkillMatrixKey = ["skill-matrices", "current"] as const;

export function useCurrentSkillMatrix() {
  return useQuery({
    queryKey: currentSkillMatrixKey,
    queryFn: getCurrentSkillMatrix
  });
}
