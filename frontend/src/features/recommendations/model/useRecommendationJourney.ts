import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { archiveRoadmap, getCurrentRecommendations, getRoadmaps } from "../api/recommendationApi";
export const recommendationJourneyKey=["recommendation-journey","current"] as const;
export function useRecommendationJourney(){return useQuery({queryKey:recommendationJourneyKey,queryFn:async()=>{const [recommendations,roadmaps]=await Promise.all([getCurrentRecommendations(),getRoadmaps()]);const roadmap=roadmaps.find(item=>item.status==="CREATED"||item.status==="IN_PROGRESS")??null;return {recommendations,roadmap,roadmaps};}});}
export function useArchiveRoadmap(){const client=useQueryClient();return useMutation({mutationFn:archiveRoadmap,onSuccess:()=>client.invalidateQueries({queryKey:recommendationJourneyKey})});}
