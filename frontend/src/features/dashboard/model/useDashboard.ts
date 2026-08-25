import { useQuery } from "@tanstack/react-query";
import { getDashboardSummary } from "../api/dashboardApi";

export const dashboardKey = ["dashboard", "summary"] as const;

export function useDashboard() {
  return useQuery({ queryKey: dashboardKey, queryFn: getDashboardSummary });
}
