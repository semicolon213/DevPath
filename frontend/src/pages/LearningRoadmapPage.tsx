import { useRef, useState } from "react";
import { LearningRecommendationPanel } from "../features/ai/ui/LearningRecommendationPanel";
import type { Recommendation, RoadmapStep } from "../features/recommendations/api/recommendationApi";
import { useArchiveRoadmap, useRecommendationJourney } from "../features/recommendations/model/useRecommendationJourney";
import { ApiError } from "../shared/api/apiClient";
import { categoryLabel, difficultyLabel, priorityLabel, recommendationTypeLabel, roadmapStatusLabel, roadmapStepStatusLabel } from "../shared/ui/labels";
import { ConfirmDialog } from "../shared/ui/ConfirmDialog";
import { PageState } from "../shared/ui/PageState";
import { PageHero } from "../shared/ui/PageHero";
import { ResultBasis } from "../shared/ui/ResultBasis";

export function LearningRoadmapPage() {
  const query = useRecommendationJourney();
  const archive = useArchiveRoadmap();
  const [confirmingArchive, setConfirmingArchive] = useState(false);
  const [archiveMessage, setArchiveMessage] = useState("");
  const archiveMessageRef = useRef<HTMLParagraphElement>(null);
  if (query.isPending) return <PageState state="loading" title="학습 로드맵" message="추천과 학습 로드맵을 불러오는 중입니다." />;
  if (query.isError) return query.error instanceof ApiError && query.error.status === 404 ? <PageState state="empty" title="아직 학습 로드맵이 없습니다" description="지원 직무를 선택하고 새 분석을 완료하면 자동 생성됩니다." actionTo="/repositories" actionLabel="저장소 확인" /> : <PageState state="error" title="학습 로드맵을 표시할 수 없습니다" errorTitle="학습 로드맵을 불러오지 못했습니다" error={query.error} retry={() => query.refetch()} />;
  const { recommendations, roadmap, roadmaps } = query.data;
  return (
    <>
      <PageHero>
        <div>
          <p className="eyebrow">공식 학습 계획</p>
          <h1>학습 로드맵</h1>
          <p>공식 역량 차이와 승인된 정책으로 생성된 로드맵입니다. 선택적 AI 학습 가이드는 아래 별도 영역에서 제공합니다.</p>
        </div>
      </PageHero>
      {roadmap === null ? (
        <section className="state-panel">
          <h2>진행 중인 로드맵이 없습니다</h2>
          <p>새 분석이 완료되면 현재 추천에서 새 로드맵이 생성됩니다.</p>
        </section>
      ) : (
        <>
          <section className="roadmap-summary" aria-label="로드맵 요약">
            <div>
              <strong>{recommendations.recommendations.length}</strong>
              <span>현재 추천</span>
            </div>
            <div>
              <strong>{roadmap.steps.length}</strong>
              <span>학습 단계</span>
            </div>
            <div>
              <strong>{formatNumber(roadmap.progressPercent)}%</strong>
              <span>진행률</span>
              <progress aria-label="로드맵 진행률" max="100" value={roadmap.progressPercent}>
                {formatNumber(roadmap.progressPercent)}%
              </progress>
            </div>
          </section>
          {recommendations.recommendations.length === 0 ? (
            <section className="state-panel">
              <h2>현재 개선 추천이 없습니다</h2>
              <p>현재 필수 범주가 모두 충분 이상입니다. 새 분석이 완료되면 정책 기준으로 다시 생성됩니다.</p>
            </section>
          ) : (
            <>
              <section aria-labelledby="recommendations-title">
                <div className="section-heading">
                  <div>
                    <h2 id="recommendations-title">우선 추천</h2>
                    <p>관찰 없음 → 약함 → 일부 충족 순서와 직무 가중치·선행 순서를 반영합니다.</p>
                  </div>
                </div>
                <ol className="roadmap-recommendation-list">
                  {recommendations.recommendations.map((item) => (
                    <RecommendationRow key={item.recommendationId} item={item} />
                  ))}
                </ol>
              </section>
              <section aria-labelledby="roadmap-title">
                <div className="section-heading">
                  <div>
                    <h2 id="roadmap-title">실행 순서</h2>
                    <p>앞 단계가 다음 단계의 명시적 선행 단계로 연결됩니다.</p>
                  </div>
                </div>
                <ol className="roadmap-step-list">
                  {roadmap.steps.map((step) => (
                    <StepCard key={step.roadmapStepId} step={step} />
                  ))}
                </ol>
              </section>
            </>
          )}
          <div className="roadmap-actions">
            <button
              type="button"
              className="button-secondary button-danger-secondary"
              disabled={archive.isPending}
              onClick={() => {
                archive.reset();
                setArchiveMessage("");
                setConfirmingArchive(true);
              }}
            >
              {archive.isPending ? "보관 중…" : "현재 로드맵 보관"}
            </button>
          </div>
        </>
      )}
      <p ref={archiveMessageRef} role="status" tabIndex={-1}>
        {archiveMessage}
      </p>
      <ConfirmDialog
        open={confirmingArchive}
        title="현재 로드맵을 보관할까요?"
        description="로드맵 내용과 이력은 유지됩니다. 다음 분석이 완료되면 새 로드맵이 생성될 수 있습니다."
        confirmLabel="로드맵 보관"
        destructive
        pending={archive.isPending}
        error={archive.isError ? "로드맵을 보관하지 못했습니다. 다시 시도해 주세요." : undefined}
        returnFocusFallback={archiveMessageRef}
        onCancel={() => setConfirmingArchive(false)}
        onConfirm={() => {
          if (roadmap)
            archive.mutate(roadmap.roadmapId, {
              onSuccess: () => {
                setArchiveMessage("로드맵을 보관했습니다.");
                setConfirmingArchive(false);
              },
            });
        }}
      />
      <section aria-labelledby="roadmap-history-title" className="roadmap-history">
        <div className="section-heading">
          <div>
            <h2 id="roadmap-history-title">로드맵 이력</h2>
            <p>정책 버전과 당시 진행 상태를 보존합니다.</p>
          </div>
        </div>
        {roadmaps.length === 0 ? (
          <p>저장된 로드맵이 없습니다.</p>
        ) : (
          <ul>
            {roadmaps.map((item) => (
              <li key={item.roadmapId}>
                <div>
                  <strong>{formatDateTime(item.generatedAt)} 로드맵</strong>
                  <span>{roadmapStatusLabel(item.status)}</span>
                </div>
                <p>
                  단계 {item.steps.length}개 · 진행률 {formatNumber(item.progressPercent)}%
                </p>
                <time dateTime={item.updatedAt}>
                  {new Intl.DateTimeFormat("ko-KR", {
                    dateStyle: "medium",
                    timeStyle: "short",
                  }).format(new Date(item.updatedAt))}
                </time>
              </li>
            ))}
          </ul>
        )}
      </section>
      {roadmap !== null && recommendations.recommendations.length > 0 && <LearningRecommendationPanel learningRoadmapId={roadmap.roadmapId} />}
      <ResultBasis items={[{ label: "추천 정책", value: recommendations.policyVersion }, ...(roadmap ? [{ label: "로드맵 정책", value: roadmap.policyVersion }, { label: "기준 시각", value: <time dateTime={roadmap.updatedAt}>{formatDateTime(roadmap.updatedAt)}</time> }] : [])]} />
    </>
  );
}
function RecommendationRow({ item }: { item: Recommendation }) {
  return (
    <li className="roadmap-recommendation">
      <div>
        <span>{categoryLabel(item.category)}</span>
        <strong>{priorityLabel(item.priority)}</strong>
      </div>
      <h3>{item.title}</h3>
      <p>{item.completionCriteria}</p>
      <small>
        {recommendationTypeLabel(item.type)} · 예상 {item.effortHours}시간 · 근거 {item.evidenceIds.length}개
      </small>
    </li>
  );
}
function StepCard({ step }: { step: RoadmapStep }) {
  const status = stepStatus(step.status);
  return (
    <li className={`roadmap-step roadmap-step--${step.status.toLowerCase()}`}>
      <span className="roadmap-step__index">{step.position + 1}</span>
      <article>
        <div>
          <h3>{step.title}</h3>
          <strong>{difficultyLabel(step.difficulty)}</strong>
        </div>
        <p className="roadmap-step__status">
          <span aria-hidden="true">{status.icon}</span>
          {status.label}
        </p>
        <p>{step.completionCriteria}</p>
        <details>
          <summary>필요한 측정 근거</summary>
          <ul>
            {step.expectedEvidence.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </details>
        <small>
          예상 {step.effortHours}시간 · 선행 단계 {step.prerequisiteStepIds.length}개
        </small>
      </article>
    </li>
  );
}
function formatNumber(value: number) {
  return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value);
}
function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
function stepStatus(value: RoadmapStep["status"]) {
  return { COMPLETED: { icon: "✓", label: roadmapStepStatusLabel(value) }, IN_PROGRESS: { icon: "◷", label: roadmapStepStatusLabel(value) }, NOT_STARTED: { icon: "○", label: roadmapStepStatusLabel(value) }, SKIPPED: { icon: "↷", label: roadmapStepStatusLabel(value) } }[value];
}
