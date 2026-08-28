import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { getCurrentSkillMatrix, getSkillHistoryPage, getSkillMatrixComparison, getSkillWorkspace } from "../api/skillMatrixApi";

export const currentSkillMatrixKey = ["skill-matrices", "current"] as const;

export function useCurrentSkillMatrix() {
  return useQuery({
    queryKey: currentSkillMatrixKey,
    queryFn: getCurrentSkillMatrix
  });
}

export function useSkillMatrixComparison(skillMatrixIds: string[]) {
  const valid = skillMatrixIds.length === 2 && skillMatrixIds[0] !== skillMatrixIds[1];
  return useQuery({
    queryKey: ["skill-matrices", "compare", ...skillMatrixIds],
    queryFn: () => getSkillMatrixComparison(skillMatrixIds),
    enabled: valid
  });
}

export function useSkillWorkspace(skillId: string | undefined) {
  return useQuery({
    queryKey: ["skills", skillId, "workspace"],
    queryFn: () => getSkillWorkspace(skillId!),
    enabled: Boolean(skillId)
  });
}

export function useSkillHistory(skillId: string | undefined) {
  return useInfiniteQuery({
    queryKey: ["skills", skillId, "history"],
    queryFn: ({ pageParam }) => getSkillHistoryPage(skillId!, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: page => page.nextCursor ?? undefined,
    enabled: Boolean(skillId)
  });
}
