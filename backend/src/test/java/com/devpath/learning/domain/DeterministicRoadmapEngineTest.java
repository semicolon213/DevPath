package com.devpath.learning.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.recommendation.domain.Recommendation;
import com.devpath.recommendation.domain.RecommendationPriority;
import com.devpath.recommendation.domain.RecommendationSet;
import com.devpath.recommendation.domain.RecommendationType;
import com.devpath.rule.domain.RuleCategory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeterministicRoadmapEngineTest {
    @Test
    void buildsStableSequentialMilestonesAndPrerequisites() {
        UUID policyId=UUID.randomUUID();UUID roadmapId=UUID.randomUUID();Instant now=Instant.parse("2026-08-25T00:00:00Z");
        RecommendationSet set=new RecommendationSet(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),policyId,
            "recommendation-v1","PUBLISHED",List.of(recommendation(RuleCategory.FRAMEWORK,0,20),recommendation(RuleCategory.LANGUAGE,1,12)),now);
        RoadmapPolicy policy=new RoadmapPolicy(UUID.randomUUID(),"roadmap-v1",policyId,
            java.util.Map.of(RuleCategory.LANGUAGE,0,RuleCategory.FRAMEWORK,1));
        var engine=new DeterministicRoadmapEngine();

        LearningRoadmap first=engine.generate(roadmapId,set,policy,now);
        LearningRoadmap repeated=engine.generate(roadmapId,set,policy,now);

        assertThat(repeated).isEqualTo(first);
        assertThat(first.steps()).extracting(RoadmapStep::category).containsExactly(RuleCategory.LANGUAGE,RuleCategory.FRAMEWORK);
        assertThat(first.steps().getFirst().prerequisiteStepIds()).isEmpty();
        assertThat(first.steps().get(1).prerequisiteStepIds()).containsExactly(first.steps().getFirst().stepId());
        assertThat(first.milestones()).hasSameSizeAs(first.steps());
    }
    private Recommendation recommendation(RuleCategory category,int position,int effort){return new Recommendation(UUID.randomUUID(),UUID.randomUUID(),category,RecommendationType.PROJECT,RecommendationPriority.HIGH,"CAREER_REQUIRED_GAP","Title","Reach 60",List.of("evidence"),List.of(UUID.randomUUID()),effort,position,"PROPOSED");}
}
