import { Link, Route, Routes } from "react-router-dom";
import { HomePage } from "../pages/HomePage";
import { NotFoundPage } from "../pages/NotFoundPage";
import { RepositoriesPage } from "../pages/RepositoriesPage";
import { RepositoryDetailPage } from "../pages/RepositoryDetailPage";
import { SkillsPage } from "../pages/SkillsPage";
import { AnalysesPage } from "../pages/AnalysesPage";
import { AnalysisDetailPage } from "../pages/AnalysisDetailPage";
import { CareersPage } from "../pages/CareersPage";
import { CareerDetailPage } from "../pages/CareerDetailPage";
import { CompaniesPage } from "../pages/CompaniesPage";
import { CompanyDetailPage } from "../pages/CompanyDetailPage";
import { DashboardPage } from "../pages/DashboardPage";
import { CareerReadinessPage } from "../pages/CareerReadinessPage";
import { LearningRoadmapPage } from "../pages/LearningRoadmapPage";
export function AppRoutes(){return <Routes><Route path="/" element={<HomePage/>}/><Route path="/dashboard" element={<DashboardPage/>}/><Route path="/repositories" element={<RepositoriesPage/>}/><Route path="/repositories/:repositoryId" element={<RepositoryDetailPage/>}/><Route path="/skills" element={<SkillsPage/>}/><Route path="/career-readiness" element={<CareerReadinessPage/>}/><Route path="/roadmap" element={<LearningRoadmapPage/>}/><Route path="/analyses" element={<AnalysesPage/>}/><Route path="/analyses/:analysisId" element={<AnalysisDetailPage/>}/><Route path="/careers" element={<CareersPage/>}/><Route path="/careers/:careerId" element={<CareerDetailPage/>}/><Route path="/companies" element={<CompaniesPage/>}/><Route path="/companies/:id" element={<CompanyDetailPage/>}/><Route path="*" element={<NotFoundPage/>}/></Routes>}
export function HomeLink(){return <Link to="/">DevPath 홈으로 돌아가기</Link>}
