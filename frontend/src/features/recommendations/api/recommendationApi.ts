import { apiRequest } from "../../../shared/api/apiClient";

export type Recommendation = { recommendationId:string;skillGapId:string;category:string;type:"STUDY"|"PROJECT"|"ARCHITECTURE"|"PORTFOLIO";priority:"CRITICAL"|"HIGH"|"MEDIUM"|"LOW";rationaleCode:string;title:string;completionCriteria:string;expectedEvidence:string[];evidenceIds:string[];effortHours:number;position:number;status:"PROPOSED"|"ACCEPTED"|"DISMISSED"|"COMPLETED" };
export type RecommendationSet = { recommendationSetId:string;careerReadinessId:string;policyVersion:string;status:"PUBLISHED"|"SUPERSEDED";recommendations:Recommendation[];generatedAt:string };
export type RoadmapMilestone = { milestoneId:string;position:number;category:string;title:string;status:"PLANNED"|"ACHIEVED"|"SKIPPED" };
export type RoadmapStep = { roadmapStepId:string;milestoneId:string;recommendationId:string;position:number;category:string;title:string;difficulty:"BEGINNER"|"INTERMEDIATE"|"ADVANCED";effortHours:number;prerequisiteStepIds:string[];completionCriteria:string;expectedEvidence:string[];status:"NOT_STARTED"|"IN_PROGRESS"|"COMPLETED"|"SKIPPED" };
export type LearningRoadmap = { roadmapId:string;recommendationSetId:string;policyVersion:string;status:"CREATED"|"IN_PROGRESS"|"COMPLETED"|"ARCHIVED";progressPercent:number;milestones:RoadmapMilestone[];steps:RoadmapStep[];generatedAt:string;updatedAt:string };
export type RecommendationEvidence = { evidenceId:string;evidenceType:string;sourceReference:string;observedFactSummary:string;confidence:number;createdAt:string };
export async function getCurrentRecommendations(){return (await apiRequest<RecommendationSet>("/api/v1/recommendations/current")).data;}
export async function getRecommendationSets(){return (await apiRequest<{recommendationSets:RecommendationSet[]}>("/api/v1/recommendations")).data.recommendationSets;}
export async function getRecommendation(recommendationId:string){return (await apiRequest<Recommendation>(`/api/v1/recommendations/${recommendationId}`)).data;}
export async function getRecommendationEvidence(recommendationId:string){return (await apiRequest<{recommendationId:string;evidence:RecommendationEvidence[]}>(`/api/v1/recommendations/${recommendationId}/evidence`)).data;}
export async function getActiveRoadmap(){return (await apiRequest<LearningRoadmap>("/api/v1/learning-roadmaps/active")).data;}
export async function getRoadmaps(){return (await apiRequest<{roadmaps:LearningRoadmap[]}>("/api/v1/learning-roadmaps")).data.roadmaps;}
export async function archiveRoadmap(roadmapId:string){const csrf=await apiRequest<{headerName:string;token:string}>("/api/v1/csrf");return (await apiRequest<LearningRoadmap>(`/api/v1/learning-roadmaps/${roadmapId}/archive`,{method:"POST",headers:{[csrf.data.headerName]:csrf.data.token,"Idempotency-Key":createId(),"Content-Type":"application/json"},body:JSON.stringify({})})).data;}
function createId(){const randomUUID=globalThis.crypto?.randomUUID;return typeof randomUUID==="function"?randomUUID.call(globalThis.crypto):`roadmap-${Date.now()}-${Math.random().toString(36).slice(2)}`;}
