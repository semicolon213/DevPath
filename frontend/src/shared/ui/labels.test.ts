import { describe, expect, it } from "vitest";
import {
  categoryLabel, chunkStatusLabel, evidenceTypeLabel, jobStatusLabel, knowledgeFreshnessLabel,
  evidenceStatusLabel, levelLabel, priorityLabel, recommendationStatusLabel, recommendationTypeLabel,
  repositoryLifecycleLabel, repositorySyncStatusLabel, safeJobErrorMessage, skillNameLabel
} from "./labels";

describe("shared Korean labels", () => {
  const tables = [
    [categoryLabel, ["LANGUAGE", "FRAMEWORK", "DATABASE", "ARCHITECTURE", "TESTING", "DEVOPS", "DOCUMENTATION", "ACTIVITY", "COLLABORATION"]],
    [levelLabel, ["NONE", "BEGINNER", "DEVELOPING", "COMPETENT", "STRONG"]],
    [evidenceStatusLabel, ["NONE", "INSUFFICIENT", "SUFFICIENT"]],
    [priorityLabel, ["CRITICAL", "HIGH", "MEDIUM", "LOW"]],
    [recommendationTypeLabel, ["STUDY", "PROJECT", "ARCHITECTURE", "PORTFOLIO"]],
    [recommendationStatusLabel, ["PROPOSED", "ACCEPTED", "DISMISSED", "COMPLETED"]],
    [jobStatusLabel, ["queued", "running", "succeeded", "failed", "cancelled", "expired"]],
    [evidenceTypeLabel, ["REPOSITORY_PATH", "LANGUAGE_STATISTIC", "SNAPSHOT_SIGNAL", "PROVIDER_LANGUAGE_STATISTICS", "DEPENDENCY_DECLARATION"]],
    [knowledgeFreshnessLabel, ["FRESH"]],
    [chunkStatusLabel, ["INDEXED", "STALE", "DELETED"]],
    [repositoryLifecycleLabel, ["DISCOVERED", "ACTIVE", "ARCHIVED", "DELETED_EXTERNALLY"]],
    [repositorySyncStatusLabel, ["NOT_SYNCED", "SYNCHRONIZED", "FAILED"]],
    [skillNameLabel, ["language-engineering", "framework-application", "database-engineering", "architecture-structure", "testing-discipline", "delivery-operations", "technical-documentation", "development-activity"]]
  ] as const;

  it.each(tables)("maps every known value without exposing the raw value", (formatter, values) => {
    values.forEach(value => {
      expect(formatter(value)).not.toBe(value);
      expect(formatter(value)).not.toBe("알 수 없음");
    });
  });

  it("normalizes job status casing and hides unknown values", () => {
    expect(jobStatusLabel("SUCCEEDED")).toBe("완료");
    expect(categoryLabel("NEW_INTERNAL_VALUE")).toBe("알 수 없음");
  });

  it("maps every evidence status and uses a neutral fallback", () => {
    expect(evidenceStatusLabel("NONE")).toBe("근거 없음");
    expect(evidenceStatusLabel("INSUFFICIENT")).toBe("근거 부족");
    expect(evidenceStatusLabel("SUFFICIENT")).toBe("근거 충분");
    expect(evidenceStatusLabel("NEW_STATUS")).toBe("알 수 없음");
    expect(evidenceStatusLabel(null)).toBe("알 수 없음");
  });

  it("uses an API skill name only when the local skill label is unknown", () => {
    expect(skillNameLabel("testing-discipline", "API Testing Name")).toBe("테스트 역량");
    expect(skillNameLabel("new-skill", "새 기술")).toBe("새 기술");
    expect(skillNameLabel("new-skill")).toBe("알 수 없음");
  });

  it("turns job error codes into safe retry guidance", () => {
    expect(safeJobErrorMessage("RATE_LIMIT_EXCEEDED", true)).toBe("외부 서비스 요청 한도에 도달했습니다. 잠시 후 다시 시도해 주세요.");
    expect(safeJobErrorMessage("PROVIDER_SECRET_FAILURE", false)).toBe("작업을 완료하지 못했습니다. 연결과 접근 권한을 확인한 뒤 다시 시도해 주세요.");
  });
});
