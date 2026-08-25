import { useQuery } from "@tanstack/react-query";
import { getRecommendation, getRecommendationEvidence, getRecommendationSets } from "../api/recommendationApi";

export function useRecommendationHistory(){return useQuery({queryKey:["recommendations","history"],queryFn:getRecommendationSets});}
export function useRecommendationDetail(recommendationId:string){return useQuery({queryKey:["recommendations","detail",recommendationId],queryFn:async()=>{const [recommendation,evidence]=await Promise.all([getRecommendation(recommendationId),getRecommendationEvidence(recommendationId)]);return {recommendation,evidence:evidence.evidence};}});}
