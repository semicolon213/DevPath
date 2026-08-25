import { useQuery } from "@tanstack/react-query";
import { getActiveRoadmap, getCurrentRecommendations } from "../api/recommendationApi";
export const recommendationJourneyKey=["recommendation-journey","current"] as const;
export function useRecommendationJourney(){return useQuery({queryKey:recommendationJourneyKey,queryFn:async()=>{const [recommendations,roadmap]=await Promise.all([getCurrentRecommendations(),getActiveRoadmap()]);return {recommendations,roadmap};}});}
