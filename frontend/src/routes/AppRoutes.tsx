import { createBrowserRouter, Outlet, useRouteError, type RouteObject } from "react-router-dom";
import { AppLayout } from "../app/AppLayout";
import { SafeErrorPage } from "../app/ErrorBoundary";
import { useEffect } from "react";
import { AnalysisComparisonPage } from "../pages/AnalysisComparisonPage";
import { AnalysisDetailPage } from "../pages/AnalysisDetailPage";
import { AnalysesPage } from "../pages/AnalysesPage";
import { CareerDetailPage } from "../pages/CareerDetailPage";
import { CareerReadinessDetailPage } from "../pages/CareerReadinessDetailPage";
import { CareerReadinessPage } from "../pages/CareerReadinessPage";
import { CareersPage } from "../pages/CareersPage";
import { CompaniesPage } from "../pages/CompaniesPage";
import { CompanyDetailPage } from "../pages/CompanyDetailPage";
import { DashboardPage } from "../pages/DashboardPage";
import { HomePage } from "../pages/HomePage";
import { IntegrationSettingsPage } from "../pages/IntegrationSettingsPage";
import { KnowledgeDetailPage } from "../pages/KnowledgeDetailPage";
import { KnowledgePage } from "../pages/KnowledgePage";
import { LearningRoadmapPage } from "../pages/LearningRoadmapPage";
import { NotFoundPage } from "../pages/NotFoundPage";
import { OnboardingPage } from "../pages/OnboardingPage";
import { ProfileSettingsPage } from "../pages/ProfileSettingsPage";
import { RecommendationDetailPage } from "../pages/RecommendationDetailPage";
import { RecommendationsPage } from "../pages/RecommendationsPage";
import { RepositoriesPage } from "../pages/RepositoriesPage";
import { RepositoryDetailPage } from "../pages/RepositoryDetailPage";
import { RepositorySnapshotDetailPage } from "../pages/RepositorySnapshotDetailPage";
import { SettingsPage } from "../pages/SettingsPage";
import { SkillDetailPage } from "../pages/SkillDetailPage";
import { SkillMatrixComparisonPage } from "../pages/SkillMatrixComparisonPage";
import { SkillsPage } from "../pages/SkillsPage";
import { RouteAccessibility } from "../shared/ui/RouteAccessibility";

function RouteFrame() {
  return <><RouteAccessibility /><Outlet /></>;
}

export function RouteErrorBoundary() {
  const error = useRouteError();
  useEffect(() => {
    console.error("DevPath route error boundary captured an error.", error);
  }, [error]);
  return <SafeErrorPage />;
}

export const appRoutes: RouteObject[] = [{
  element: <RouteFrame />,
  errorElement: <RouteErrorBoundary />,
  children: [
    { index: true, element: <HomePage /> },
    {
      element: <AppLayout />,
      children: [
        { path: "onboarding", element: <OnboardingPage /> },
        { path: "settings", element: <SettingsPage /> },
        { path: "settings/profile", element: <ProfileSettingsPage /> },
        { path: "settings/integrations", element: <IntegrationSettingsPage /> },
        { path: "dashboard", element: <DashboardPage /> },
        { path: "repositories", element: <RepositoriesPage /> },
        { path: "repositories/:repositoryId", element: <RepositoryDetailPage /> },
        { path: "repositories/:repositoryId/snapshots/:snapshotId", element: <RepositorySnapshotDetailPage /> },
        { path: "knowledge", element: <KnowledgePage /> },
        { path: "knowledge/:documentId", element: <KnowledgeDetailPage /> },
        { path: "skills", element: <SkillsPage /> },
        { path: "skills/compare", element: <SkillMatrixComparisonPage /> },
        { path: "skills/:skillId", element: <SkillDetailPage /> },
        { path: "career-readiness", element: <CareerReadinessPage /> },
        { path: "career-readiness/:careerReadinessId", element: <CareerReadinessDetailPage /> },
        { path: "recommendations", element: <RecommendationsPage /> },
        { path: "recommendations/:recommendationId", element: <RecommendationDetailPage /> },
        { path: "roadmap", element: <LearningRoadmapPage /> },
        { path: "analyses", element: <AnalysesPage /> },
        { path: "analyses/compare", element: <AnalysisComparisonPage /> },
        { path: "analyses/:analysisId", element: <AnalysisDetailPage /> },
        { path: "careers", element: <CareersPage /> },
        { path: "careers/:careerId", element: <CareerDetailPage /> },
        { path: "companies", element: <CompaniesPage /> },
        { path: "companies/:id", element: <CompanyDetailPage /> }
      ]
    },
    { path: "*", element: <NotFoundPage /> }
  ]
}];

export const appRouter = createBrowserRouter(appRoutes);
