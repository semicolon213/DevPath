import { expect, test, type Page, type Route } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

const timestamp = "2026-08-25T00:00:00Z";
const metadata = { requestId: "e2e-request", apiVersion: "v1", timestamp };
const repository = {
  repositoryId: "repository-id", providerRepositoryId: "42", name: "devpath", fullName: "owner/devpath",
  owner: "owner", visibility: "PUBLIC", defaultBranch: "main", providerArchived: false, lifecycle: "ACTIVE",
  syncStatus: "SYNCHRONIZED", htmlUrl: "https://github.com/owner/devpath", discoveredAt: timestamp,
  lastSyncedAt: timestamp, currentSnapshotId: "snapshot-id"
};
const skillMatrix = {
  skillMatrixId: "matrix-id", evaluationId: "evaluation-id", policyVersion: "skill-matrix-v2",
  ruleSetVersion: "baseline-v2", status: "CURRENT", strengths: [], weaknesses: ["testing-discipline"],
  generatedAt: timestamp,
  skills: [{
    assessmentId: "assessment-id", skillId: "skill-id", skillKey: "testing-discipline",
    skillName: "Testing Discipline", category: "TESTING", score: 50, level: "DEVELOPING", confidence: 90,
    strength: false, weakness: true, growthTrend: "UNAVAILABLE",
    aggregateRuleResultReference: "evaluation-id:TESTING", evidenceIds: ["evidence-id"],
    repositoryIds: [repository.repositoryId], recommendationInputFacts: ["TEST_FILES"], ruleSetVersion: "baseline-v2"
  }]
};
const previousSkillMatrix = {
  ...skillMatrix, skillMatrixId: "matrix-previous-id", evaluationId: "evaluation-previous-id", status: "SUPERSEDED",
  strengths: [], weaknesses: [], skills: skillMatrix.skills.map(value => ({
    ...value, assessmentId: "assessment-previous-id", score: 55, level: "DEVELOPING", weakness: false,
    evidenceIds: ["evidence-previous-id"]
  }))
};

type CapturedAnalysisRequest = { csrf?: string; idempotencyKey?: string; body: unknown };

test("profile and GitHub integration settings remain separate and accessible", async ({ page }) => {
  await installApiFixture(page, []);
  await page.goto("/settings");
  await expect(page.getByRole("heading", { name: "내 DevPath 설정" })).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "프로필 설정 열기" }).click();
  await expect(page.getByRole("textbox", { name: "표시 이름" })).toHaveValue("개발자");
  await expect(page.getByRole("combobox", { name: "목표 직무" })).toHaveValue("backend");
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "GitHub 연결" }).click();
  await expect(page.getByText("미연결")).toBeVisible();
  await expect(page.getByRole("button", { name: "GitHub 저장소 연결" })).toBeVisible();
  await expectNoAccessibilityViolations(page);
});

test("completed onboarding hands the first analysis journey to result workspaces", async ({ page }) => {
  await installApiFixture(page, []);
  await page.goto("/onboarding");

  await expect(page.getByRole("heading", { name: "첫 분석 준비를 마쳤습니다" })).toBeVisible();
  await expect(page.getByLabel("전체 8단계 중 7단계 완료")).toBeVisible();
  await expect(page.getByRole("list", { name: "온보딩 단계" })).toContainText("목표 회사");
  await expect(page.getByRole("link", { name: "Skill Matrix" })).toHaveAttribute("href", "/skills");
  await expectNoAccessibilityViolations(page);
});

test("dashboard summary presents authoritative results with accessible partial-state semantics", async ({ page }) => {
  await installApiFixture(page, []);
  await page.goto("/dashboard");

  await expect(page.getByRole("heading", { name: "내 DevPath 대시보드" })).toBeVisible();
  await expect(page.getByText("백엔드 개발자")).toBeVisible();
  await expect(page.getByText("테스트 자동화 강화")).toBeVisible();
  await expect(page.getByRole("heading", { name: "최근 작업" })).toBeVisible();
  await page.keyboard.press("Tab");
  await expect(page.getByRole("link", { name: "본문으로 건너뛰기" })).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("main")).toBeFocused();
  await expectNoAccessibilityViolations(page);
});

test("repository analysis result flows into skills, readiness, and roadmap", async ({ page }) => {
  const captured: CapturedAnalysisRequest[] = [];
  await installApiFixture(page, captured);

  await page.goto(`/repositories/${repository.repositoryId}`);
  await expect(page.getByRole("heading", { name: repository.fullName })).toBeVisible();
  await expect(page.getByRole("heading", { name: "감지된 기술 스택" })).toBeVisible();
  await expect(page.getByText("React", { exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "엔지니어링 증거" })).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("button", { name: "결정론적 분석 시작" }).click();
  await expect(page.getByText("분석이 완료되었습니다.")).toBeVisible();
  expect(captured).toHaveLength(1);
  expect(captured[0]).toMatchObject({
    csrf: "e2e-csrf-token",
    body: { repositoryId: repository.repositoryId, analysisScope: "REPOSITORY_BASELINE" }
  });
  expect(captured[0].idempotencyKey).toBeTruthy();

  await page.getByRole("link", { name: "분석 이력 보기" }).click();
  await expect(page).toHaveURL(/\/analyses$/);
  await expect(page.getByRole("heading", { name: "분석 이력" })).toBeFocused();
  await page.getByRole("link", { name: "결과 상세" }).click();
  await expect(page).toHaveURL(/\/analyses\/analysis-id$/);
  await expect(page.getByRole("heading", { name: repository.fullName })).toBeFocused();
  await expect(page.getByRole("heading", { name: "왜 이 점수가 나왔나요?" })).toBeVisible();
  await expect(page.getByText("82.5", { exact: true })).toBeVisible();
  await expect(page.getByText("README 파일을 확인했습니다.")).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.goto("/analyses/compare?analysisId=analysis-previous-id&analysisId=analysis-id");
  await expect(page.getByRole("heading", { name: "분석 결과 비교" })).toBeVisible();
  await expect(page.getByRole("row", { name: /테스트 55/ })).toContainText("없음");
  await expect(page.getByText("82.5", { exact: true })).toBeVisible();
  await expect(page.getByText(/차이 점수나 향상률을 새로 계산하지 않습니다/)).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "기술 역량 비교" }).click();
  await expect(page).toHaveURL(/\/skills\/compare/);
  await expect(page.getByRole("heading", { name: "기술 역량 비교" })).toBeFocused();
  const testingSkillRow = page.getByRole("row").filter({ has: page.getByRole("rowheader", { name: /Testing Discipline/ }) });
  await expect(testingSkillRow).toContainText("55");
  await expect(testingSkillRow).toContainText("50");
  await expect(page.getByText(/차이 점수나 성장 추세를 새로 계산하지 않습니다/)).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.goto("/skills");
  await expect(page.getByRole("heading", { name: "기술 역량 분석" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "테스트 역량" })).toBeVisible();
  await expect(page.getByText("정책 skill-matrix-v2")).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "테스트 역량" }).click();
  await expect(page).toHaveURL(/\/skills\/skill-id$/);
  await expect(page.getByRole("heading", { name: "Testing Discipline" })).toBeFocused();
  await expect(page.getByText("50", { exact: true })).toBeVisible();
  await expect(page.getByText("테스트 파일 근거가 확인되었습니다.")).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.goto("/career-readiness");
  await expect(page.getByRole("heading", { name: "커리어 준비도" })).toBeVisible();
  await expect(page.getByText("67.5", { exact: true })).toBeVisible();
  await expect(page.getByText("정책 readiness-v1")).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "추천 로드맵 보기" }).click();
  await expect(page).toHaveURL(/\/roadmap$/);
  await expect(page.getByRole("heading", { name: "추천 및 학습 로드맵" })).toBeVisible();
  await expect(page.getByText("추천 recommendation-v1")).toBeVisible();
  await expect(page.getByText("로드맵 roadmap-v1")).toBeVisible();
  await expect(page.getByRole("heading", { name: "테스트 자동화 강화" }).first()).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.goto("/recommendations");
  await expect(page.getByRole("heading", { name: "추천 작업공간" })).toBeVisible();
  await expect(page.getByText("recommendation-v1")).toBeVisible();
  await page.getByRole("link", { name: "준비도 근거 보기" }).click();
  await expect(page).toHaveURL(/\/career-readiness\/career-readiness-id$/);
  await expect(page.getByRole("heading", { name: "커리어 준비도 상세" })).toBeFocused();
  await expect(page.getByText("67.5", { exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "기술 평가와 증거 보기" })).toHaveAttribute("href", "/skills/skill-id");
  await expectNoAccessibilityViolations(page);
  await page.goto("/recommendations");
  await page.getByRole("link", { name: "상세 근거 보기" }).click();
  await expect(page).toHaveURL(/\/recommendations\/recommendation-id$/);
  await expect(page.getByRole("heading", { name: "테스트 자동화 강화" })).toBeFocused();
  await expect(page.getByText("README 파일을 확인했습니다.")).toBeVisible();
  await expectNoAccessibilityViolations(page);
});

test("anonymous readiness failure is actionable and accessible", async ({ page }) => {
  await page.route("http://localhost:8080/api/v1/career-readiness/current", route => route.fulfill({
    status: 401,
    contentType: "application/json",
    body: JSON.stringify({ code: "UNAUTHORIZED" })
  }));

  await page.goto("/career-readiness");
  await expect(page.getByRole("alert")).toContainText("로그인이 필요합니다");
  await expect(page.getByRole("link", { name: "로그인 화면으로 이동" })).toHaveAttribute("href", "/");
  await expectNoAccessibilityViolations(page);
});

async function installApiFixture(page: Page, captured: CapturedAnalysisRequest[]) {
  await page.route("http://localhost:8080/api/v1/**", async route => {
    const request = route.request();
    const key = `${request.method()} ${new URL(request.url()).pathname}`;
    if (key === "POST /api/v1/analyses") {
      captured.push({
        csrf: request.headers()["x-xsrf-token"],
        idempotencyKey: request.headers()["idempotency-key"],
        body: request.postDataJSON()
      });
    }
    if (!(key in responses)) {
      await route.fulfill({ status: 500, contentType: "application/json", body: JSON.stringify({ error: key }) });
      return;
    }
    await fulfill(route, responses[key]);
  });
}

const responses: Record<string, unknown> = {
  "GET /api/v1/users/me/profile": { profileId: "profile-id", displayName: "개발자", careerStage: "JUNIOR", bio: "백엔드", updatedAt: timestamp },
  "GET /api/v1/users/me/preferences": { careerId: "backend", companyId: null, updatedAt: timestamp },
  "GET /api/v1/users/me/connections": { connections: [] },
  "GET /api/v1/careers": { careers: [{ careerId: "backend", localizedName: "백엔드 개발자", profileVersion: "career-v2" }] },
  "GET /api/v1/companies": { companies: [{ companyId: "naver", localizedName: "네이버", profileVersion: "company-v1" }] },
  "GET /api/v1/users/me/onboarding-progress": {
    status: "DASHBOARD_READY", completedStepCount: 7, totalStepCount: 8, nextStep: "DASHBOARD_READY",
    generatedAt: timestamp,
    steps: [
      onboardingStep("ACCOUNT", "REQUIRED", "COMPLETE"),
      onboardingStep("PROFILE", "REQUIRED", "COMPLETE"),
      onboardingStep("CAREER_TARGET", "RECOMMENDED", "COMPLETE"),
      onboardingStep("COMPANY_TARGET", "OPTIONAL", "INCOMPLETE"),
      onboardingStep("GITHUB_CONNECTION", "REQUIRED", "COMPLETE"),
      onboardingStep("REPOSITORY_IMPORT", "REQUIRED", "COMPLETE"),
      onboardingStep("INITIAL_SYNC", "REQUIRED", "COMPLETE"),
      onboardingStep("INITIAL_ANALYSIS", "RECOMMENDED", "COMPLETE")
    ]
  },
  "GET /api/v1/dashboard/summary": {
    generatedAt: timestamp,
    targets: { status: "AVAILABLE", career: { id: "backend", localizedName: "백엔드 개발자", profileVersion: "career-v2" }, company: null },
    repositories: { status: "AVAILABLE", totalCount: 1, synchronizedCount: 1, recent: [{ repositoryId: repository.repositoryId, fullName: repository.fullName, syncStatus: "SYNCHRONIZED", lastSyncedAt: timestamp }] },
    analyses: { status: "AVAILABLE", totalCount: 1, latest: { analysisId: "analysis-id", repositoryId: repository.repositoryId, repositoryFullName: repository.fullName, overallScore: 82.5, confidence: 95, currentForRepository: true, completedAt: timestamp }, currentByRepository: [{ analysisId: "analysis-id", repositoryId: repository.repositoryId, repositoryFullName: repository.fullName, overallScore: 82.5, confidence: 95, currentForRepository: true, completedAt: timestamp }] },
    skillOverview: { status: "AVAILABLE", skillMatrixId: "matrix-id", skillCount: 1, strengthCount: 0, weaknessCount: 1, policyVersion: "skill-matrix-v2", ruleSetVersion: "baseline-v2", generatedAt: timestamp },
    readiness: { status: "AVAILABLE", careerReadinessId: "career-readiness-id", resultStatus: "COMPLETED", score: 67.5, level: "COMPETENT", confidence: 82, unavailableCategories: [], assessedAt: timestamp },
    recommendations: { status: "AVAILABLE", recommendationSetId: "recommendation-set-id", policyVersion: "recommendation-v1", items: [{ recommendationId: "recommendation-id", category: "TESTING", type: "PROJECT", priority: "HIGH", title: "테스트 자동화 강화", effortHours: 16, position: 0, status: "PROPOSED" }], generatedAt: timestamp },
    roadmap: { status: "AVAILABLE", roadmapId: "roadmap-id", policyVersion: "roadmap-v1", resultStatus: "CREATED", progressPercent: 0, milestoneCount: 1, stepCount: 1, updatedAt: timestamp },
    recentJobs: { status: "AVAILABLE", items: [{ jobId: "analysis-job-id", jobType: "ANALYSIS", repositoryId: repository.repositoryId, status: "SUCCEEDED", phase: "COMPLETED", progressPercent: 100, submittedAt: timestamp, completedAt: timestamp }] }
  },
  "GET /api/v1/csrf": { headerName: "X-XSRF-TOKEN", token: "e2e-csrf-token" },
  "GET /api/v1/repositories/repository-id": repository,
  "GET /api/v1/repositories/repository-id/snapshots": { snapshots: [{
    snapshotId: "snapshot-id", repositoryId: repository.repositoryId, capturedAt: timestamp,
    sourceRevision: "abc123", status: "READY", immutable: true, contentHash: "sha256:e2e",
    branchCount: 2, commitCount: 42
  }] },
  "GET /api/v1/repositories/repository-id/technologies": {
    repositoryId: repository.repositoryId, snapshotId: "snapshot-id",
    extractorVersion: "repository-technology-summary-v1", taxonomyVersion: "technology-taxonomy-v1",
    primaryLanguage: "TypeScript", technologies: [{
      name: "React", category: "FRAMEWORK", evidenceLabel: "react", byteCount: null, percentage: null,
      taxonomyStatus: "SUPPORTED", evidenceType: "DEPENDENCY_DECLARATION", evidencePaths: ["frontend/package.json"]
    }]
  },
  "GET /api/v1/repositories/repository-id/evidence": {
    repositoryId: repository.repositoryId, snapshotId: "snapshot-id", extractorVersion: "engineering-evidence-extractor-v2",
    categories: [{ category: "TESTING", label: "Testing", signals: [{
      signalKey: "TEST_FILES", label: "Test files", present: true, count: 12,
      observedValue: null, evidencePaths: ["frontend/src/App.test.tsx"]
    }] }]
  },
  "POST /api/v1/analyses": analysisJob("queued", "QUEUED", 0, null),
  "GET /api/v1/analysis-jobs/analysis-job-id": analysisJob("succeeded", "COMPLETED", 100, "/api/v1/analyses/analysis-id"),
  "GET /api/v1/analyses": {
    analyses: [{
      analysisId: "analysis-id", repositoryId: repository.repositoryId, snapshotId: "snapshot-id",
      evaluationId: "evaluation-id", skillMatrixId: "matrix-id", analysisScope: "REPOSITORY_BASELINE",
      currentForRepository: true, completedAt: timestamp, repositoryFullName: repository.fullName,
      overallScore: 82.5, confidence: 95, ruleSetVersion: "baseline-v2", policyVersion: "skill-matrix-v2"
    }], limit: 20, nextCursor: null, totalCount: 1
  },
  "GET /api/v1/analyses/analysis-id": {
    analysisId: "analysis-id", repositoryId: repository.repositoryId, snapshotId: "snapshot-id",
    evaluationId: "evaluation-id", skillMatrixId: "matrix-id", analysisScope: "REPOSITORY_BASELINE",
    currentForRepository: true, completedAt: timestamp
  },
  "GET /api/v1/analyses/compare": {
    analyses: [{
      analysisId: "analysis-previous-id", repositoryId: repository.repositoryId, snapshotId: "snapshot-previous-id",
      evaluationId: "evaluation-previous-id", skillMatrixId: "matrix-previous-id", analysisScope: "REPOSITORY_BASELINE",
      currentForRepository: false, completedAt: timestamp, repositoryFullName: repository.fullName,
      overallScore: 70, confidence: 88, ruleSetVersion: "baseline-v2", policyVersion: "skill-matrix-v2"
    }, {
      analysisId: "analysis-id", repositoryId: repository.repositoryId, snapshotId: "snapshot-id",
      evaluationId: "evaluation-id", skillMatrixId: "matrix-id", analysisScope: "REPOSITORY_BASELINE",
      currentForRepository: true, completedAt: timestamp, repositoryFullName: repository.fullName,
      overallScore: 82.5, confidence: 95, ruleSetVersion: "baseline-v2", policyVersion: "skill-matrix-v2"
    }]
  },
  "GET /api/v1/rule-evaluations/evaluation-previous-id": {
    evaluationId: "evaluation-previous-id", snapshotId: "snapshot-previous-id", ruleSetVersionId: "rule-set-version-id",
    ruleSetVersion: "baseline-v2", formulaLibraryVersion: "formula-v1", extractorVersion: "extractor-v2",
    overallScore: 70, confidence: 88, evidenceSummary: { evidenceCount: 1, rulesWithEvidence: 1, missingEvidenceCount: 0 },
    categoryScores: [{ category: "TESTING", score: 55, weight: 0.2, confidence: 88, ruleResults: [{
      ruleId: "TEST_FILES", ruleVersion: "1.0", status: "PARTIAL", rawValue: 1, score: 55,
      weight: 1, formulaId: "BOOLEAN_100", trace: "TEST_FILES=1", evidenceReferences: ["tests"]
    }], missingEvidence: [] }], warnings: [], completedAt: timestamp
  },
  "GET /api/v1/rule-evaluations/evaluation-id": {
    evaluationId: "evaluation-id", snapshotId: "snapshot-id", ruleSetVersionId: "rule-set-version-id",
    ruleSetVersion: "baseline-v2", formulaLibraryVersion: "formula-v1", extractorVersion: "extractor-v2",
    overallScore: 82.5, confidence: 95,
    evidenceSummary: { evidenceCount: 1, rulesWithEvidence: 1, missingEvidenceCount: 0 },
    categoryScores: [{
      category: "DOCUMENTATION", score: 100, weight: 0.2, confidence: 100,
      ruleResults: [{
        ruleId: "README_PRESENT", ruleVersion: "1.0", status: "PASSED", rawValue: 1, score: 100,
        weight: 1, formulaId: "BOOLEAN_100", trace: "README_PRESENT=1", evidenceReferences: ["README.md"]
      }], missingEvidence: []
    }], warnings: [], completedAt: timestamp
  },
  "GET /api/v1/rule-evaluations/evaluation-id/evidence": { evaluationId: "evaluation-id", evidence: [{
    evidenceId: "evidence-id", ruleId: "README_PRESENT", contributionRole: "DIRECT",
    evidenceType: "REPOSITORY_PATH", sourceReference: "README.md",
    observedFactSummary: "README 파일을 확인했습니다.", confidence: 100
  }] },
  "GET /api/v1/skill-matrices/matrix-id": skillMatrix,
  "GET /api/v1/skill-matrices/current": skillMatrix,
  "GET /api/v1/skill-matrices/compare": { matrices: [previousSkillMatrix, skillMatrix] },
  "GET /api/v1/skills/skill-id": {
    skillMatrixId: "matrix-id", evaluationId: "evaluation-id", policyVersion: "skill-matrix-v2",
    ruleSetVersion: "baseline-v2", matrixStatus: "CURRENT", generatedAt: timestamp, skill: skillMatrix.skills[0]
  },
  "GET /api/v1/skills/skill-id/evidence": {
    skillId: "skill-id", skillAssessmentId: "assessment-id", skillMatrixId: "matrix-id", evidence: [{
      evidenceId: "evidence-id", snapshotId: "snapshot-id", evidenceType: "REPOSITORY_PATH",
      sourceReference: "snapshot:snapshot-id:path:src/test", observedFactSummary: "테스트 파일 근거가 확인되었습니다.", confidence: 100
    }]
  },
  "GET /api/v1/career-readiness/current": {
    careerReadinessId: "career-readiness-id", skillMatrixId: "matrix-id", careerId: "backend",
    careerProfileVersionId: "career-profile-version-id", careerProfileVersion: "career-v2",
    readinessPolicyVersion: "readiness-v1", ruleSetVersion: "baseline-v2", status: "COMPLETED",
    readinessScore: 67.5, readinessLevel: "COMPETENT", confidence: 82, unavailableCategories: [], assessedAt: timestamp,
    skillGaps: [{
      skillGapId: "skill-gap-id", skillId: "skill-id", skillKey: "testing-discipline", category: "TESTING",
      actualScore: 50, actualLevel: "DEVELOPING", expectedMinimum: 60, gapState: "PARTIAL",
      careerWeight: 20, evidenceIds: ["evidence-id"]
    }]
  },
  "GET /api/v1/career-readiness/career-readiness-id": {
    careerReadinessId: "career-readiness-id", skillMatrixId: "matrix-id", careerId: "backend",
    careerProfileVersionId: "career-profile-version-id", careerProfileVersion: "career-v2",
    readinessPolicyVersion: "readiness-v1", ruleSetVersion: "baseline-v2", status: "COMPLETED",
    readinessScore: 67.5, readinessLevel: "COMPETENT", confidence: 82, unavailableCategories: [], assessedAt: timestamp,
    skillGaps: [{ skillGapId: "skill-gap-id", skillId: "skill-id", skillKey: "testing-discipline", category: "TESTING",
      actualScore: 50, actualLevel: "DEVELOPING", expectedMinimum: 60, gapState: "PARTIAL",
      careerWeight: 20, evidenceIds: ["evidence-id"] }]
  },
  "GET /api/v1/career-readiness/career-readiness-id/skill-gaps": {
    careerReadinessId: "career-readiness-id", skillGaps: [{ skillGapId: "skill-gap-id", skillId: "skill-id",
      skillKey: "testing-discipline", category: "TESTING", actualScore: 50, actualLevel: "DEVELOPING",
      expectedMinimum: 60, gapState: "PARTIAL", careerWeight: 20, evidenceIds: ["evidence-id"] }]
  },
  "GET /api/v1/recommendations/current": {
    recommendationSetId: "recommendation-set-id", careerReadinessId: "career-readiness-id",
    policyVersion: "recommendation-v1", status: "PUBLISHED", generatedAt: timestamp,
    recommendations: [{
      recommendationId: "recommendation-id", skillGapId: "skill-gap-id", category: "TESTING", type: "PROJECT",
      priority: "HIGH", rationaleCode: "CAREER_REQUIRED_GAP", title: "테스트 자동화 강화",
      completionCriteria: "공식 Testing 점수 60 이상과 테스트 파일 근거를 확보합니다.",
      expectedEvidence: ["test files"], evidenceIds: ["evidence-id"], effortHours: 16, position: 0, status: "PROPOSED"
    }]
  },
  "GET /api/v1/recommendations": {
    recommendationSets: [{
      recommendationSetId: "recommendation-set-id", careerReadinessId: "career-readiness-id",
      policyVersion: "recommendation-v1", status: "PUBLISHED", generatedAt: timestamp,
      recommendations: [{
        recommendationId: "recommendation-id", skillGapId: "skill-gap-id", category: "TESTING", type: "PROJECT",
        priority: "HIGH", rationaleCode: "CAREER_REQUIRED_GAP", title: "테스트 자동화 강화",
        completionCriteria: "공식 Testing 점수 60 이상과 테스트 파일 근거를 확보합니다.",
        expectedEvidence: ["test files"], evidenceIds: ["evidence-id"], effortHours: 16, position: 0, status: "PROPOSED"
      }]
    }]
  },
  "GET /api/v1/recommendations/recommendation-id": {
    recommendationId: "recommendation-id", skillGapId: "skill-gap-id", category: "TESTING", type: "PROJECT",
    priority: "HIGH", rationaleCode: "CAREER_REQUIRED_GAP", title: "테스트 자동화 강화",
    completionCriteria: "공식 Testing 점수 60 이상과 테스트 파일 근거를 확보합니다.",
    expectedEvidence: ["test files"], evidenceIds: ["evidence-id"], effortHours: 16, position: 0, status: "PROPOSED"
  },
  "GET /api/v1/recommendations/recommendation-id/evidence": {
    recommendationId: "recommendation-id", evidence: [{ evidenceId: "evidence-id", evidenceType: "SNAPSHOT_SIGNAL",
      sourceReference: "README.md", observedFactSummary: "README 파일을 확인했습니다.", confidence: 100, createdAt: timestamp }]
  },
  "GET /api/v1/learning-roadmaps/active": {
    roadmapId: "roadmap-id", recommendationSetId: "recommendation-set-id", policyVersion: "roadmap-v1",
    status: "CREATED", progressPercent: 0, generatedAt: timestamp, updatedAt: timestamp,
    milestones: [{ milestoneId: "milestone-id", position: 0, category: "TESTING", title: "테스트 자동화 강화", status: "PLANNED" }],
    steps: [{
      roadmapStepId: "roadmap-step-id", milestoneId: "milestone-id", recommendationId: "recommendation-id",
      position: 0, category: "TESTING", title: "테스트 자동화 강화", difficulty: "INTERMEDIATE",
      effortHours: 16, prerequisiteStepIds: [],
      completionCriteria: "공식 Testing 점수 60 이상과 테스트 파일 근거를 확보합니다.",
      expectedEvidence: ["test files"], status: "NOT_STARTED"
    }]
  }
  ,"GET /api/v1/learning-roadmaps": {
    roadmaps: [{
      roadmapId: "roadmap-id", recommendationSetId: "recommendation-set-id", policyVersion: "roadmap-v1",
      status: "CREATED", progressPercent: 0, generatedAt: timestamp, updatedAt: timestamp,
      milestones: [{ milestoneId: "milestone-id", position: 0, category: "TESTING", title: "Add automated testing", status: "PLANNED" }],
      steps: [{
        roadmapStepId: "roadmap-step-id", milestoneId: "milestone-id", recommendationId: "recommendation-id",
        position: 0, category: "TESTING", title: "Add automated testing", difficulty: "INTERMEDIATE",
        effortHours: 16, prerequisiteStepIds: [], completionCriteria: "Reach the measurable Testing target.",
        expectedEvidence: ["test files"], status: "NOT_STARTED"
      }]
    }]
  }
};

function analysisJob(status: string, phase: string, progressPercent: number, resultResourceUrl: string | null) {
  return {
    jobId: "analysis-job-id", jobType: "REPOSITORY_ANALYSIS", status, phase, progressPercent,
    attemptCount: status === "queued" ? 0 : 1, maxAttempts: 3, submittedAt: timestamp,
    startedAt: status === "queued" ? null : timestamp, completedAt: status === "succeeded" ? timestamp : null,
    pollingUrl: "/api/v1/analysis-jobs/analysis-job-id", resultResourceUrl,
    errorCode: null, errorMessage: null, retryable: false
  };
}

function onboardingStep(step: string, requirement: string, status: string) {
  return { step, requirement, status, resourceId: null, actionPath: "/onboarding" };
}

async function fulfill(route: Route, data: unknown) {
  await route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({ data, metadata })
  });
}

async function expectNoAccessibilityViolations(page: Page) {
  const result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
    .analyze();
  const details = result.violations.map(violation => ({
    id: violation.id,
    impact: violation.impact,
    targets: violation.nodes.flatMap(node => node.target)
  }));
  expect(result.violations, JSON.stringify(details, null, 2)).toEqual([]);
}
