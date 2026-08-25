import { useQuery } from "@tanstack/react-query";
import { getDashboardView } from "../api/dashboardApi";

export const dashboardKey = ["dashboard", "composed-view"] as const;

export function useDashboard() {
  return useQuery({ queryKey: dashboardKey, queryFn: getDashboardView });
}
