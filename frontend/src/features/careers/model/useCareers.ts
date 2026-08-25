import { useQuery } from "@tanstack/react-query";
import { getCareer, getCareers } from "../api/careerApi";

export const careersKey = ["careers"] as const;
export function useCareers() { return useQuery({ queryKey: careersKey, queryFn: getCareers }); }
export function useCareer(careerId?: string) {
  return useQuery({ queryKey: [...careersKey, careerId], queryFn: () => getCareer(careerId!), enabled: Boolean(careerId) });
}
