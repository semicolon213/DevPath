import type { CareerCatalog } from "../../careers/api/careerApi";

type ReadinessTargetLabelOptions = {
  careers: CareerCatalog | undefined;
  careerId: string;
  profileVersion: string;
  pending: boolean;
  prefix: "목표" | "평가 직무";
  companyName?: string;
};

export function readinessTargetLabel({ careers, careerId, profileVersion, pending, prefix, companyName }: ReadinessTargetLabelOptions) {
  if (pending) return `${prefix} 확인 중`;
  const careerName = careers?.careers.find(career => career.careerId === careerId)?.localizedName;
  return careerName ? `${prefix} · ${careerName}${companyName ? ` · ${companyName}` : ""}` : `프로필 ${profileVersion}`;
}
