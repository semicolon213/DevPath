import { apiRequest } from "../../../shared/api/apiClient";

export type Recommendation = { recommendationId:string;skillGapId:string;category:string;type:"STUDY"|"PROJECT"|"ARCHITECTURE"|"PORTFOLIO";priority:"CRITICAL"|"HIGH"|"MEDIUM"|"LOW";rationaleCode:string;title:string;completionCriteria:string;expectedEvidence:string[];evidenceIds:string[];effortHours:number;position:number;status:"PROPOSED"|"ACCEPTED"|"DISMISSED"|"COMPLETED" };
export type RecommendationSet = { recommendationSetId:string;careerReadinessId:string;policyVersion:string;status:"PUBLISHED"|"SUPERSEDED";recommendations:Recommendation[];generatedAt:string };
export type RoadmapMilestone = { milestoneId:string;position:number;category:string;title:string;status:"PLANNED"|"ACHIEVED"|"SKIPPED" };
export type RoadmapStep = { roadmapStepId:string;milestoneId:string;recommendationId:string;position:number;category:string;title:string;difficulty:"BEGINNER"|"INTERMEDIATE"|"ADVANCED";effortHours:number;prerequisiteStepIds:string[];completionCriteria:string;expectedEvidence:string[];status:"NOT_STARTED"|"IN_PROGRESS"|"COMPLETED"|"SKIPPED" };
export type LearningRoadmap = { roadmapId:string;recommendationSetId:string;policyVersion:string;status:"CREATED"|"IN_PROGRESS"|"COMPLETED"|"ARCHIVED";progressPercent:number;milestones:RoadmapMilestone[];steps:RoadmapStep[];generatedAt:string;updatedAt:string };
export async function getCurrentRecommendations(){return (await apiRequest<RecommendationSet>("/api/v1/recommendations/current")).data;}
export async function getActiveRoadmap(){return (await apiRequest<LearningRoadmap>("/api/v1/learning-roadmaps/active")).data;}
