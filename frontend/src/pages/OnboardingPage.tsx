import { Link } from "react-router-dom";
import { ProfilePanel } from "../features/profile/ui/ProfilePanel";
import { ConnectionPanel } from "../features/connections/ui/ConnectionPanel";
import { useOnboardingProgress } from "../features/onboarding/model/useOnboarding";
import type { OnboardingStep, OnboardingStepName } from "../features/onboarding/api/onboardingApi";
import { PageState } from "../shared/ui/PageState";
import { PageHero } from "../shared/ui/PageHero";

const labels: Record<OnboardingStepName, { title: string; description: string }> = {
  ACCOUNT: {
    title: "DevPath 계정",
    description: "GitHub 로그인과 안전한 서버 세션을 확인합니다.",
  },
  PROFILE: {
    title: "기본 프로필",
    description: "표시 이름과 경력 단계를 관리합니다.",
  },
  CAREER_TARGET: {
    title: "목표 직무",
    description: "직무별 결정론적 평가 기준을 선택합니다.",
  },
  COMPANY_TARGET: {
    title: "목표 회사",
    description: "선택 사항이며 나중에 설정해도 됩니다.",
  },
  GITHUB_CONNECTION: {
    title: "GitHub 저장소 연결",
    description: "서버가 저장소 접근 권한을 안전하게 보관합니다.",
  },
  REPOSITORY_IMPORT: {
    title: "분석 저장소 등록",
    description: "허용된 GitHub 저장소를 작업 공간에 추가합니다.",
  },
  INITIAL_SYNC: {
    title: "첫 저장소 동기화",
    description: "불변 스냅샷과 측정 가능한 근거를 수집합니다.",
  },
  INITIAL_ANALYSIS: {
    title: "첫 결정론적 분석",
    description: "Rule Engine이 공식 점수와 Skill Matrix를 생성합니다.",
  },
};

export function OnboardingPage() {
  const progress = useOnboardingProgress();
  if (progress.isPending) return <PageState state="loading" title="DevPath 시작하기" message="온보딩 진행 상태를 확인하는 중입니다…" />;
  if (progress.isError) return <PageState state="error" title="온보딩 상태를 확인할 수 없습니다" description="저장된 진행 상태는 변경되지 않았습니다." error={progress.error} retry={() => progress.refetch()} />;
  const ready = progress.data.status === "DASHBOARD_READY";
  const nextIndex = progress.data.steps.findIndex((step) => step.step === progress.data.nextStep);
  const next = progress.data.nextStep === "DASHBOARD_READY" ? null : (progress.data.steps.find((step) => step.step === progress.data.nextStep) ?? null);
  return (
    <>
      <PageHero>
        <div>
          <p className="eyebrow">첫 분석 여정</p>
          <h1>{ready ? "첫 분석 준비를 마쳤습니다" : "DevPath 시작하기"}</h1>
          <p>{ready ? "저장소 근거와 결정론적 분석 결과가 준비되었습니다. 이제 대시보드에서 다음 행동을 이어가세요." : "목표 설정부터 GitHub 근거 수집과 첫 분석까지 한 흐름으로 진행합니다."}</p>
        </div>
        <div className="workspace-actions">
          <button type="button" className="button-secondary" disabled={progress.isFetching} onClick={() => progress.refetch()}>
            {progress.isFetching ? "상태 확인 중…" : "진행 상태 새로고침"}
          </button>
          {ready ? (
            <Link className="button-link button-primary" to="/dashboard">
              대시보드 열기
            </Link>
          ) : null}
        </div>
      </PageHero>
      <section className="onboarding-summary" aria-labelledby="onboarding-progress-title">
        <div>
          <p className="eyebrow">저장된 진행 상태</p>
          <h2 id="onboarding-progress-title">설정 진행 상태</h2>
          <p>서버에 저장된 목표, 연결, 저장소, 동기화 및 분석 결과를 기준으로 확인합니다.</p>
        </div>
        <div className="onboarding-count" aria-label={`전체 ${progress.data.totalStepCount}단계 중 ${progress.data.completedStepCount}단계 완료`}>
          <strong>{progress.data.completedStepCount}</strong>
          <span>/ {progress.data.totalStepCount} 완료</span>
        </div>
      </section>
      {next ? (
        <section className="next-step-card" aria-labelledby="next-step-title">
          <div>
            <p className="eyebrow">지금 할 일</p>
            <h2 id="next-step-title">{labels[next.step].title}</h2>
            <p>{labels[next.step].description}</p>
          </div>
          <Link className="button-link button-primary" to={next.actionPath}>
            이 단계 진행
          </Link>
        </section>
      ) : null}
      <ol className="onboarding-steps" aria-label="온보딩 단계">
        {progress.data.steps.map((step, index) => (
          <StepCard key={step.step} step={step} position={index + 1} current={step.step === progress.data.nextStep} beforeCurrent={index < nextIndex} />
        ))}
      </ol>
      {!ready ? (
        <section className="onboarding-controls" aria-labelledby="onboarding-controls-title">
          <div className="section-heading">
            <div>
              <h2 id="onboarding-controls-title">설정과 GitHub 저장소 등록</h2>
              <p>변경 내용은 서버에 저장됩니다. 완료 후 위의 상태 새로고침으로 다음 단계를 확인하세요.</p>
            </div>
          </div>
          <ProfilePanel embedded />
          <ConnectionPanel embedded />
        </section>
      ) : (
        <section className="onboarding-ready-actions" aria-labelledby="ready-actions-title">
          <h2 id="ready-actions-title">분석 결과에서 이어가기</h2>
          <div className="workspace-actions">
            <Link className="button-link" to="/dashboard">
              대시보드
            </Link>
            <Link className="button-link button-secondary" to="/skills">
              Skill Matrix
            </Link>
            <Link className="button-link button-secondary" to="/career-readiness">
              직무 준비도
            </Link>
            <Link className="button-link button-secondary" to="/recommendations">
              추천
            </Link>
          </div>
        </section>
      )}
      <p className="muted">마지막 확인 {formatDateTime(progress.data.generatedAt)} · 목표 회사는 선택 사항이며 미선택 상태가 분석을 막지 않습니다.</p>
    </>
  );
}

function StepCard({ step, position, current, beforeCurrent }: { step: OnboardingStep; position: number; current: boolean; beforeCurrent: boolean }) {
  const label = labels[step.step];
  const complete = step.status === "COMPLETE";
  return (
    <li className={`onboarding-step onboarding-step--${complete ? "complete" : current ? "current" : beforeCurrent ? "incomplete" : "upcoming"}`}>
      <span className="onboarding-step__number" aria-hidden="true">
        {complete ? "✓" : position}
      </span>
      <div>
        <div className="onboarding-step__title">
          <h3>{label.title}</h3>
          <span className={`status-badge ${complete ? "status-badge--active" : current ? "status-badge--current" : ""}`}>{complete ? "완료" : current ? "지금" : beforeCurrent ? "미완료" : "예정"}</span>
          <small>{requirementLabel(step.requirement)}</small>
        </div>
        <p>{label.description}</p>
      </div>
    </li>
  );
}
function requirementLabel(value: OnboardingStep["requirement"]) {
  return value === "REQUIRED" ? "필수" : value === "RECOMMENDED" ? "권장" : "선택";
}
function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
