import { chromium, expect, test, type Page, type Route } from "@playwright/test";
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
    evidenceStatus: "INSUFFICIENT",
    strength: false, weakness: true, growthTrend: "UNAVAILABLE",
    aggregateRuleResultReference: "evaluation-id:TESTING", evidenceIds: ["evidence-id"],
    repositoryIds: [repository.repositoryId], recommendationInputFacts: ["TEST_FILES"], ruleSetVersion: "baseline-v2"
  }]
};
const previousSkillMatrix = {
  ...skillMatrix, skillMatrixId: "matrix-previous-id", evaluationId: "evaluation-previous-id", status: "SUPERSEDED",
  strengths: [], weaknesses: [], skills: skillMatrix.skills.map(value => ({
    ...value, assessmentId: "assessment-previous-id", score: 55, level: "DEVELOPING", weakness: false,
    evidenceStatus: "NONE",
    evidenceIds: ["evidence-previous-id"]
  }))
};

type CapturedAnalysisRequest = { csrf?: string; idempotencyKey?: string; body: unknown };
type CapturedKnowledgeRequest = { csrf?: string; idempotencyKey?: string; body: unknown };
type CapturedKnowledgeSearch = { csrf?: string; body: unknown };

test("mobile app navigation is keyboard operable and accessible", async ({ page }) => {
  await installApiFixture(page, []);
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto("/dashboard");
  await expect(page.getByRole("heading", { name: "내 DevPath 대시보드" })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(375);

  const menu = page.getByRole("button", { name: "메뉴" });
  await menu.focus();
  await page.keyboard.press("Enter");
  await expect(menu).toHaveAttribute("aria-expanded", "true");
  await page.keyboard.press("Tab");
  const navigation = page.getByRole("navigation", { name: "주요 메뉴" });
  await expect(navigation.getByRole("link", { name: "대시보드" })).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(navigation.getByRole("link", { name: "저장소" })).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(navigation.getByRole("link", { name: "분석" })).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(/\/analyses$/);
  await expect(menu).toHaveAttribute("aria-expanded", "false");
  await expectNoAccessibilityViolations(page);
  await expectNoShellLandmarkViolations(page);
});

test("dashboard has no horizontal overflow with desktop scrollbars visible", async () => {
  const browser = await chromium.launch({ headless: true, ignoreDefaultArgs: ["--hide-scrollbars"] });
  try {
    for (const width of [1440, 1280, 1180, 900, 721]) {
      const context = await browser.newContext({ baseURL: test.info().project.use.baseURL, viewport: { width, height: 900 } });
      const page = await context.newPage();
      await installApiFixture(page, []);
      await page.goto("/dashboard");
      await expect(page.getByRole("heading", { name: "내 DevPath 대시보드" })).toBeVisible();
      const size = await page.evaluate(() => ({ clientWidth: document.documentElement.clientWidth, scrollWidth: document.documentElement.scrollWidth }));
      expect(size.scrollWidth).toBeLessThanOrEqual(size.clientWidth);
      await context.close();
    }
  } finally {
    await browser.close();
  }
});

test("changed result pages have no horizontal overflow and keep single-column mobile rows", async () => {
  const browser = await chromium.launch({ headless: true, ignoreDefaultArgs: ["--hide-scrollbars"] });
  try {
    for (const width of [1440, 1280, 721, 375]) {
      const context = await browser.newContext({ baseURL: test.info().project.use.baseURL, viewport: { width, height: 900 } });
      const page = await context.newPage();
      await installApiFixture(page, []);
      for (const [path, heading] of [["/analyses/analysis-id", "이 분석의 역량 매트릭스"], ["/skills", "테스트 역량"], ["/roadmap", "실행 순서"], ["/recommendations", "추천 세트 이력"], [`/repositories/${repository.repositoryId}`, "감지된 기술 스택"], ["/skills/skill-id", "평가 추적 정보"]]) {
        await page.goto(path);
        await expect(page.getByRole("heading", { name: heading })).toBeVisible();
        const size = await page.evaluate(() => ({ clientWidth: document.documentElement.clientWidth, scrollWidth: document.documentElement.scrollWidth }));
        expect(size.scrollWidth, `${path} at ${width}px`).toBeLessThanOrEqual(size.clientWidth);
        if (path === "/analyses/analysis-id" && width >= 1280) {
          const slots = await page.locator(".historical-skill-row").evaluateAll(rows => rows.map(row => ["name", "score", "status"].map(slot => Math.round(row.querySelector(`.historical-skill-row__${slot}`)!.getBoundingClientRect().left))));
          for (const row of slots) expect(row, `historical slots at ${width}px`).toEqual(slots[0]);
          expect(slots[0][0]).toBeLessThan(slots[0][1]);
          expect(slots[0][1]).toBeLessThan(slots[0][2]);
        }
        if (path === "/analyses/analysis-id" && width === 375) {
          await expect(page.locator(".ai-generated-panel__heading").first(), "AI panel heading stacks at 375px").toHaveCSS("align-items", "stretch");
        }
        if (path === "/skills" && width === 375) {
          const columns = await page.locator(".skill-card").first().evaluate(element => getComputedStyle(element).gridTemplateColumns.split(" ").length);
          expect(columns).toBe(1);
        }
      }
      if (width === 375) {
        const longName = `owner/${"a".repeat(120)}`;
        await page.route(url => url.pathname === "/api/v1/repositories", route => fulfill(route, { repositories: [{ ...repository, fullName: longName }], limit: 20, nextCursor: null, totalCount: 1 }));
        await page.goto("/repositories");
        await expect(page.getByText(longName)).toBeVisible();
        const size = await page.evaluate(() => ({ clientWidth: document.documentElement.clientWidth, scrollWidth: document.documentElement.scrollWidth }));
        expect(size.scrollWidth, "/repositories with a long name at 375px").toBeLessThanOrEqual(size.clientWidth);
      }
      await context.close();
    }
  } finally {
    await browser.close();
  }
});

test("profile target selects with a long career name do not overflow at 375px with scrollbars visible", async () => {
  const browser = await chromium.launch({ headless: true, ignoreDefaultArgs: ["--hide-scrollbars"] });
  try {
    const context = await browser.newContext({ baseURL: test.info().project.use.baseURL, viewport: { width: 375, height: 900 } });
    const page = await context.newPage();
    await installApiFixture(page, []);
    const longName = "클라우드 네이티브 대규모 분산 시스템 백엔드 플랫폼 엔지니어링 전문 개발자";
    await page.route(url => url.pathname === "/api/v1/careers", route => fulfill(route, { careers: [{ careerId: "backend", localizedName: longName, profileVersion: "career-v2" }] }));
    await page.route(url => url.pathname === "/api/v1/users/me/onboarding-progress", route => fulfill(route, { ...(responses["GET /api/v1/users/me/onboarding-progress"] as object), status: "IN_PROGRESS", nextStep: "COMPANY_TARGET" }));
    for (const path of ["/settings/profile", "/onboarding"]) {
      await page.goto(path);
      await expect(page.locator("#career-target option", { hasText: longName })).toHaveCount(1);
      const size = await page.evaluate(() => ({ clientWidth: document.documentElement.clientWidth, scrollWidth: document.documentElement.scrollWidth }));
      expect(size.scrollWidth, `${path} at 375px`).toBeLessThanOrEqual(size.clientWidth);
    }
    await context.close();
  } finally {
    await browser.close();
  }
});

test("archived and externally deleted repository rows keep actions in the action column", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await installApiFixture(page, []);
  const rows = [
    { ...repository, lifecycle: "ARCHIVED", providerArchived: true },
    { ...repository, repositoryId: "deleted-id", fullName: "owner/deleted", lifecycle: "DELETED_EXTERNALLY" }
  ];
  await page.route(url => url.pathname === "/api/v1/repositories", route => fulfill(route, { repositories: rows, limit: 20, nextCursor: null, totalCount: 2 }));
  await page.goto("/repositories");
  await expect(page.getByText("GitHub에서 삭제된 저장소는 로컬에서 복원할 수 없습니다.")).toBeVisible();
  const head = await page.locator(".repository-table__head span").last().boundingBox();
  for (const card of await page.locator(".repository-table .repository-card").all()) {
    const identity = (await card.locator(".repository-card__identity").boundingBox())!;
    const actions = (await card.locator(".repository-card__actions").boundingBox())!;
    expect(Math.abs(actions.x - head!.x)).toBeLessThanOrEqual(1);
    expect(actions.y).toBeLessThan(identity.y + identity.height);
  }
  await expect(page.locator(".repository-table__head")).not.toContainText("최근 분석");
});

test("repository hero secondary actions stay legible on the indigo band", async ({ page }) => {
  await installApiFixture(page, []);
  const heroAction = async (name: string) => page.locator(".repository-detail-header").getByRole(name === "GitHub에서 보기" ? "link" : "button", { name }).evaluate(element => {
    const style = getComputedStyle(element);
    const band = getComputedStyle(element.closest(".skills-hero")!).backgroundColor;
    return { color: style.color, borderColor: style.borderTopColor, band };
  });
  await page.goto(`/repositories/${repository.repositoryId}`);
  await expect(page.locator(".repository-detail-header .status-badge")).toHaveText("사용 중 · 동기화 완료");
  for (const name of ["GitHub 동기화", "GitHub에서 보기"]) {
    const style = await heroAction(name);
    expect(style.band).toBe("rgb(35, 39, 122)");
    expect(style.color).toBe("rgb(255, 255, 255)");
    expect(style.borderColor).toBe("rgb(124, 130, 224)");
  }
  await expectNoAccessibilityViolations(page);

  await page.route("http://localhost:8080/api/v1/repositories/repository-id", route => fulfill(route, { ...repository, lifecycle: "ARCHIVED" }));
  await page.reload();
  const archived = await heroAction("저장소 복원");
  expect(archived.color).toBe("rgb(255, 255, 255)");
  expect(archived.borderColor).toBe("rgb(124, 130, 224)");
  expect(archived.borderColor).not.toBe(archived.band);
});

test("profile and GitHub integration settings remain separate and accessible", async ({ page }) => {
  await installApiFixture(page, []);
  await page.goto("/settings");
  await expect(page.getByRole("heading", { name: "내 DevPath 설정" })).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "프로필 설정 열기" }).click();
  await expect(page.getByRole("textbox", { name: "표시 이름" })).toHaveValue("개발자");
  await expect(page.getByRole("combobox", { name: "목표 직무" })).toHaveValue("backend");
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "외부 서비스 연결" }).click();
  await expect(page.getByLabel("외부 서비스 연결").getByText("미연결")).toBeVisible();
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

test("shared Notion page becomes a recoverable knowledge ingestion job", async ({ page }) => {
  const captured: CapturedKnowledgeRequest[] = [];
  const searches: CapturedKnowledgeSearch[] = [];
  await installApiFixture(page, [], captured, searches);
  await page.goto("/knowledge");

  await expect(page.getByRole("heading", { name: "지식 작업 공간" })).toBeVisible();
  await expect(page.getByLabel("Notion 페이지 가져오기").getByRole("heading", { name: "백엔드 학습 노트" })).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("button", { name: "지식으로 가져오기" }).click();
  await expect(page).toHaveURL(/job=knowledge-job-id/);
  await expect(page.getByText("지식 문서 수집 완료")).toBeVisible();
  await expect(page.getByRole("link", { name: "수집된 문서 보기" })).toHaveAttribute("href", "/knowledge/knowledge-document-id");
  expect(captured).toHaveLength(1);
  expect(captured[0]).toMatchObject({
    csrf: "e2e-csrf-token",
    body: { connectionId: "notion-connection-id", providerPageId: "notion-page-id" }
  });
  expect(captured[0].idempotencyKey).toBeTruthy();

  await page.reload();
  await expect(page.getByText("지식 문서 수집 완료")).toBeVisible();
  await page.getByRole("textbox", { name: "검색어" }).fill("회귀 방지");
  await page.getByRole("combobox", { name: "문서 범위" }).selectOption("knowledge-document-id");
  await page.getByRole("button", { name: "지식 검색" }).click();
  await expect(page.getByText("변경 전후 자동화 테스트를 실행합니다.")).toBeVisible();
  await expect(page.getByRole("link", { name: "Notion 원문 열기" })).toHaveAttribute("href", "https://www.notion.so/notion-page-id");
  expect(searches).toHaveLength(1);
  expect(searches[0]).toMatchObject({ csrf: "e2e-csrf-token", body: {
    query: "회귀 방지", filters: { sourceTypes: ["NOTION"], documentIds: ["knowledge-document-id"] },
    limit: 5, contextPurpose: "USER_SEARCH"
  }});
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

  await page.getByRole("link", { name: "스냅샷 상세 보기" }).click();
  await expect(page).toHaveURL(/\/repositories\/repository-id\/snapshots\/snapshot-id$/);
  await expect(page.getByRole("heading", { name: "owner/devpath 스냅샷" })).toBeFocused();
  await expect(page.getByLabel("스냅샷 적용 상태")).toContainText("현재 분석 기준");
  await expect(page.getByText("sha256:e2e")).toBeVisible();
  await expect(page.getByText(/점수나 품질 판정이 아닙니다/)).toBeVisible();
  await expect(page.getByRole("heading", { name: "이 스냅샷의 공식 분석" })).toBeVisible();
  await expect(page.getByLabel("저장된 공식 점수 82.5점")).toBeVisible();
  await expect(page.getByRole("link", { name: "공식 분석과 근거 보기" })).toHaveAttribute("href", "/analyses/analysis-id");
  await expectNoAccessibilityViolations(page);
  await page.locator(".workspace-actions").getByRole("link", { name: "저장소 상세" }).click();

  await page.getByRole("button", { name: "결정론적 분석 시작" }).click();
  await expect(page.getByText("분석이 완료되었습니다.")).toBeVisible();
  await expect(page).toHaveURL(/analysisJobId=analysis-job-id/);
  await expect(page.getByRole("link", { name: "완료된 공식 분석 보기" })).toHaveAttribute("href", "/analyses/analysis-id");
  await page.reload();
  await expect(page.getByText("분석이 완료되었습니다.")).toBeVisible();
  await expect(page.getByRole("link", { name: "완료된 공식 분석 보기" })).toHaveAttribute("href", "/analyses/analysis-id");
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

  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto("/analyses/compare?analysisId=analysis-previous-id&analysisId=analysis-id");
  await expect(page.getByRole("heading", { name: "분석 결과 비교" })).toBeVisible();
  await expect(page.getByRole("row", { name: /테스트 55/ })).toContainText("없음");
  await expect(page.getByText("82.5", { exact: true })).toBeVisible();
  await expect(page.getByText(/차이 점수나 향상률을 새로 계산하지 않습니다/)).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "기술 역량 비교" }).click();
  await expect(page).toHaveURL(/\/skills\/compare/);
  await expect(page.getByRole("heading", { name: "기술 역량 비교" })).toBeFocused();
  const testingSkillRow = page.getByRole("row").filter({ has: page.getByRole("rowheader", { name: /테스트 역량/ }) });
  await expect(testingSkillRow).toContainText("55");
  await expect(testingSkillRow).toContainText("50");
  await expect(testingSkillRow.getByText("5", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("columnheader", { name: "변화" })).toHaveCount(0);
  await expect(page.getByText(/차이 점수나 성장 추세를 새로 계산하지 않습니다/)).toBeVisible();
  await expectNoAccessibilityViolations(page);
  await page.setViewportSize({ width: 1280, height: 720 });

  await page.goto("/skills");
  await expect(page.getByRole("heading", { name: "기술 역량 분석" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "테스트 역량" })).toBeVisible();
  await expect(page.getByText("정책 skill-matrix-v2")).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "테스트 역량" }).click();
  await expect(page).toHaveURL(/\/skills\/skill-id$/);
  await expect(page.getByRole("heading", { name: "테스트 역량" })).toBeFocused();
  await expect(page.getByLabel("현재 기술 평가 요약").getByText("50", { exact: true })).toBeVisible();
  await expect(page.getByText("테스트 파일 근거가 확인되었습니다.")).toBeVisible();
  await expect(page.getByRole("heading", { name: "기술 평가 이력" })).toBeVisible();
  await expect(page.getByText("현재 Matrix")).toBeVisible();
  await expect(page.getByText(/변화량이나 성장 추세는 계산하지 않습니다/)).toBeVisible();
  await expect(page.getByRole("link", { name: "분석 근거 보기" })).toHaveAttribute("href", "/analyses/analysis-id");
  await expectNoAccessibilityViolations(page);

  await page.goto("/career-readiness");
  await expect(page.getByRole("heading", { name: "커리어 준비도" })).toBeVisible();
  await expect(page.getByText("67.5", { exact: true })).toBeVisible();
  await expect(page.getByText("정책 readiness-v1", { exact: true })).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.getByRole("link", { name: "추천 로드맵 보기" }).click();
  await expect(page).toHaveURL(/\/roadmap$/);
  await expect(page.getByRole("heading", { name: "추천 및 학습 로드맵" })).toBeVisible();
  await expect(page.getByText("추천 recommendation-v1")).toBeVisible();
  await expect(page.getByText("로드맵 roadmap-v1")).toBeVisible();
  await expect(page.getByRole("heading", { name: "테스트 자동화 강화" }).first()).toBeVisible();
  await expect(page.getByText("건너뜀")).toBeVisible();
  await expectNoAccessibilityViolations(page);

  await page.goto("/recommendations");
  await expect(page.getByRole("heading", { name: "추천 작업공간" })).toBeVisible();
  await expect(page.getByText("recommendation-v1")).toBeVisible();
  await expect(page.getByText("지금 할 일")).toBeVisible();
  await expectNoAccessibilityViolations(page);
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
  await expect(page.getByRole("link", { name: "홈으로 이동" })).toHaveAttribute("href", "/");
  await expectNoAccessibilityViolations(page);
});

test("page state error panel keeps its accent border and padding", async ({ page }) => {
  await installApiFixture(page, []);
  await page.route(url => url.pathname === "/api/v1/careers", route => route.fulfill({ status: 500, contentType: "application/json", body: JSON.stringify({ code: "INTERNAL_ERROR" }) }));
  await page.goto("/careers");
  const panel = page.locator(".page-state--error");
  await expect(panel).toBeVisible();
  await expect(panel).toHaveCSS("border-left-width", "5px");
  await expect(panel).toHaveCSS("padding-left", "56px");
});

test("reduced-motion users keep the keyboard journey without nonessential transitions", async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await installApiFixture(page, []);
  await page.goto(`/repositories/${repository.repositoryId}`);

  await page.keyboard.press("Tab");
  const skipLink = page.getByRole("link", { name: "본문으로 건너뛰기" });
  await expect(skipLink).toBeFocused();
  await expect(skipLink).toHaveCSS("transition-duration", "0s");
  await page.keyboard.press("Enter");
  await expect(page.getByRole("main")).toBeFocused();
  await expect(page.getByRole("button", { name: "결정론적 분석 시작" })).toBeEnabled();
  await expectNoAccessibilityViolations(page);
});

async function installApiFixture(
  page: Page,
  captured: CapturedAnalysisRequest[],
  capturedKnowledge: CapturedKnowledgeRequest[] = [],
  capturedSearch: CapturedKnowledgeSearch[] = []
) {
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
    if (key === "POST /api/v1/knowledge-documents/imports/notion") {
      capturedKnowledge.push({
        csrf: request.headers()["x-xsrf-token"],
        idempotencyKey: request.headers()["idempotency-key"],
        body: request.postDataJSON()
      });
    }
    if (key === "POST /api/v1/knowledge-search") {
      capturedSearch.push({ csrf: request.headers()["x-xsrf-token"], body: request.postDataJSON() });
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
  "GET /api/v1/users/me/connections": { connections: [{
    connectionId: "notion-connection-id", provider: "NOTION", status: "ACTIVE",
    scopes: ["read_content"], connectedAt: timestamp, expiresAt: null
  }] },
  "GET /api/v1/integrations/notion/workspaces": { workspaces: [{
    connectionId: "notion-connection-id", workspaceId: "notion-workspace-id", workspaceName: "개발 노트",
    workspaceIconUrl: null, status: "ACTIVE", connectedAt: timestamp, discoveredAt: timestamp,
    pages: [{ providerPageId: "notion-page-id", title: "백엔드 학습 노트", objectType: "PAGE",
      url: "https://www.notion.so/notion-page-id", lastEditedAt: timestamp, inTrash: false }]
  }] },
  "GET /api/v1/knowledge-documents": { documents: [{
    documentId: "knowledge-document-id", sourceType: "NOTION", sourceObjectId: "notion-page-id",
    title: "백엔드 학습 노트", status: "ACTIVE", currentVersionId: "knowledge-version-id", chunkCount: 1,
    createdAt: timestamp, updatedAt: timestamp
  }] },
  "POST /api/v1/knowledge-documents/imports/notion": {
    jobId: "knowledge-job-id", jobType: "KNOWLEDGE_INGESTION", status: "queued", phase: "QUEUED",
    progressPercent: 0, attemptCount: 0, maxAttempts: 3, submittedAt: timestamp, startedAt: null,
    completedAt: null, pollingUrl: "/api/v1/knowledge-ingestion-jobs/knowledge-job-id",
    resultResourceUrl: null, errorCode: null, errorMessage: null, retryable: true
  },
  "GET /api/v1/knowledge-ingestion-jobs/knowledge-job-id": {
    jobId: "knowledge-job-id", jobType: "KNOWLEDGE_INGESTION", status: "succeeded", phase: "COMPLETED",
    progressPercent: 100, attemptCount: 1, maxAttempts: 3, submittedAt: timestamp, startedAt: timestamp,
    completedAt: timestamp, pollingUrl: "/api/v1/knowledge-ingestion-jobs/knowledge-job-id",
    resultResourceUrl: "/api/v1/knowledge-documents/knowledge-document-id",
    errorCode: null, errorMessage: null, retryable: false
  },
  "POST /api/v1/knowledge-search": {
    retrievalResultId: "retrieval-result-id", retrievalType: "SEMANTIC", policyVersion: "knowledge-semantic-v1",
    contextPurpose: "USER_SEARCH", appliedFilters: { sourceTypes: ["NOTION"], documentIds: ["knowledge-document-id"] },
    resultCount: 1, durationMs: 4, generatedAt: timestamp, results: [{
      chunkId: "knowledge-chunk-id", documentId: "knowledge-document-id", documentTitle: "백엔드 학습 노트",
      sourceType: "NOTION", sourceObjectId: "notion-page-id", sourceUrl: "https://www.notion.so/notion-page-id",
      heading: "회귀 방지", excerpt: "변경 전후 자동화 테스트를 실행합니다.", relevance: 0.91,
      tokenEstimate: 20, freshness: "FRESH"
    }]
  },
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
  "GET /api/v1/repositories/repository-id/snapshots/snapshot-id": {
    snapshotId: "snapshot-id", repositoryId: repository.repositoryId, capturedAt: timestamp,
    sourceRevision: "abc123", status: "READY", immutable: true, contentHash: "sha256:e2e",
    branchCount: 2, commitCount: 42, pullRequestCount: 8, issueCount: 5, documentCount: 1
  },
  "GET /api/v1/repositories/repository-id/technologies": {
    repositoryId: repository.repositoryId, snapshotId: "snapshot-id",
    extractorVersion: "repository-technology-summary-v1", taxonomyVersion: "technology-taxonomy-v1",
    primaryLanguage: "TypeScript", technologies: [{
      name: "React", category: "FRAMEWORK", evidenceLabel: "react", byteCount: null, percentage: null,
      taxonomyStatus: "SUPPORTED", evidenceType: "DEPENDENCY_DECLARATION", evidencePaths: ["frontend/package.json"]
    }]
  },
  "GET /api/v1/repositories/repository-id/evidence": {
    repositoryId: repository.repositoryId, snapshotId: "snapshot-id", extractorVersion: "engineering-evidence-extractor-v3",
    categories: [{ category: "TESTING", label: "Testing", signals: [{
      signalKey: "TEST_FILES", label: "Test files", present: true, count: 12,
      observedValue: null, evidencePaths: ["frontend/src/App.test.tsx"]
    }] }],
    activityTimeline: {
      extractorVersion: "repository-activity-timeline-v1", scope: "CURRENT_SNAPSHOT", measuredAt: timestamp,
      latestActivityAt: timestamp, daysSinceLatestActivity: 0, totalEventCount: 1, truncated: false,
      events: [{ eventType: "COMMIT", sourceReference: "abc123", occurredAt: timestamp }]
    }
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
  "GET /api/v1/repositories/repository-id/analyses": {
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
    }, {
      roadmapStepId: "roadmap-step-skipped-id", milestoneId: "milestone-id", recommendationId: "recommendation-id",
      position: 1, category: "TESTING", title: "통합 테스트 시나리오 추가", difficulty: "ADVANCED",
      effortHours: 8, prerequisiteStepIds: ["roadmap-step-id"],
      completionCriteria: "통합 테스트 파일 근거를 확보합니다.",
      expectedEvidence: ["integration tests"], status: "SKIPPED"
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
      }, {
        roadmapStepId: "roadmap-step-skipped-id", milestoneId: "milestone-id", recommendationId: "recommendation-id",
        position: 1, category: "TESTING", title: "Add integration test scenarios", difficulty: "ADVANCED",
        effortHours: 8, prerequisiteStepIds: ["roadmap-step-id"], completionCriteria: "Collect integration test evidence.",
        expectedEvidence: ["integration tests"], status: "SKIPPED"
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

async function expectNoShellLandmarkViolations(page: Page) {
  const result = await new AxeBuilder({ page })
    .withRules(["landmark-no-duplicate-main", "landmark-main-is-top-level", "landmark-unique"])
    .analyze();
  expect(result.violations, JSON.stringify(result.violations.map(violation => violation.id), null, 2)).toEqual([]);
}
