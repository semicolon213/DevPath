package com.devpath.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.career.domain.CareerReadiness;
import com.devpath.career.domain.CareerReadinessStatus;
import com.devpath.career.domain.GapState;
import com.devpath.career.domain.SkillGap;
import com.devpath.rule.domain.RuleCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeterministicRecommendationEngineTest {
    @Test
    void generatesOnlyEligibleGapsWithApprovedPriorityAndOrdering() {
        UUID profileId=UUID.randomUUID();UUID setId=UUID.randomUUID();Instant now=Instant.parse("2026-08-25T00:00:00Z");
        var readiness=readiness(profileId,List.of(
            gap(RuleCategory.TESTING,GapState.WEAK,"20",4),
            gap(RuleCategory.LANGUAGE,GapState.MISSING,"15",0),
            gap(RuleCategory.DATABASE,GapState.PARTIAL,"20",2),
            gap(RuleCategory.ARCHITECTURE,GapState.SUFFICIENT,"15",3)));
        var policy=new RecommendationPolicy(UUID.randomUUID(),"recommendation-v1",profileId,Map.of(
            RuleCategory.LANGUAGE,template(RuleCategory.LANGUAGE,RecommendationType.STUDY,0,12),
            RuleCategory.DATABASE,template(RuleCategory.DATABASE,RecommendationType.PROJECT,2,20),
            RuleCategory.ARCHITECTURE,template(RuleCategory.ARCHITECTURE,RecommendationType.ARCHITECTURE,3,16),
            RuleCategory.TESTING,template(RuleCategory.TESTING,RecommendationType.PROJECT,4,16)));
        var engine=new DeterministicRecommendationEngine();

        RecommendationSet first=engine.generate(setId,readiness,policy,now);
        RecommendationSet repeated=engine.generate(setId,readiness,policy,now);

        assertThat(repeated).isEqualTo(first);
        assertThat(first.recommendations()).extracting(Recommendation::priority)
            .containsExactly(RecommendationPriority.CRITICAL,RecommendationPriority.HIGH,RecommendationPriority.MEDIUM);
        assertThat(first.recommendations()).extracting(Recommendation::category)
            .containsExactly(RuleCategory.LANGUAGE,RuleCategory.TESTING,RuleCategory.DATABASE);
        assertThat(first.recommendations()).noneMatch(value->value.category()==RuleCategory.ARCHITECTURE);
    }
    private RecommendationTemplate template(RuleCategory category,RecommendationType type,int order,int effort){return new RecommendationTemplate(category,type,order,effort,"Title "+category,"CAREER_REQUIRED_GAP","Reach 60",List.of("evidence"));}
    private SkillGap gap(RuleCategory category,GapState state,String weight,int order){return new SkillGap(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),category.name().toLowerCase(),category,new BigDecimal(state==GapState.MISSING?"0":state==GapState.WEAK?"30":state==GapState.PARTIAL?"50":"70"),state==GapState.MISSING?"NONE":"DEVELOPING",new BigDecimal("60"),state,new BigDecimal(weight),List.of(UUID.randomUUID()));}
    private CareerReadiness readiness(UUID profileId,List<SkillGap> gaps){return new CareerReadiness(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"backend",profileId,"career-v2",UUID.randomUUID(),"readiness-v1","baseline-v2",CareerReadinessStatus.COMPLETED,new BigDecimal("50"),"DEVELOPING",new BigDecimal("90"),List.of(),gaps,Instant.parse("2026-08-24T00:00:00Z"));}
}
