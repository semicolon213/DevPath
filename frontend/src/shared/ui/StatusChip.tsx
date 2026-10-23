import type { ReactNode } from "react";

export type StatusChipVariant = "neutral" | "info" | "success" | "warning" | "danger" | "current";

// One status chip for every status (DESIGN §4.4, D11). The text always carries the state.
export function StatusChip({ variant = "neutral", id, children }: { variant?: StatusChipVariant; id?: string; children: ReactNode }) {
  return <span id={id} className={`status-badge status-badge--${variant}`}>{children}</span>;
}

export function repositorySyncStatusVariant(value: "NOT_SYNCED" | "SYNCHRONIZED" | "FAILED"): StatusChipVariant {
  return value === "SYNCHRONIZED" ? "success" : value === "FAILED" ? "danger" : "neutral";
}
