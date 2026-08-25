package com.devpath.recommendation.adapter.out.persistence;

import com.devpath.learning.domain.LearningRoadmap;
import com.devpath.learning.domain.RoadmapMilestone;
import com.devpath.learning.domain.RoadmapPolicy;
import com.devpath.learning.domain.RoadmapStep;
import com.devpath.recommendation.application.RecommendationPersistencePort;
import com.devpath.recommendation.domain.Recommendation;
import com.devpath.recommendation.domain.RecommendationPolicy;
import com.devpath.recommendation.domain.RecommendationEvidence;
import com.devpath.recommendation.domain.RecommendationPriority;
import com.devpath.recommendation.domain.RecommendationSet;
import com.devpath.recommendation.domain.RecommendationTemplate;
import com.devpath.recommendation.domain.RecommendationType;
import com.devpath.rule.domain.RuleCategory;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JpaRecommendationAdapter implements RecommendationPersistencePort {
    private final RecommendationPolicyJpaRepository policies; private final RecommendationTemplateJpaRepository templates;
    private final RecommendationSetJpaRepository sets; private final RecommendationJpaRepository recommendations;
    private final RecommendationEvidenceJpaRepository evidence; private final RoadmapPolicyJpaRepository roadmapPolicies;
    private final LearningRoadmapJpaRepository roadmaps; private final RoadmapMilestoneJpaRepository milestones;
    private final RoadmapStepJpaRepository steps;

    public JpaRecommendationAdapter(RecommendationPolicyJpaRepository policies, RecommendationTemplateJpaRepository templates,
        RecommendationSetJpaRepository sets, RecommendationJpaRepository recommendations,
        RecommendationEvidenceJpaRepository evidence, RoadmapPolicyJpaRepository roadmapPolicies,
        LearningRoadmapJpaRepository roadmaps, RoadmapMilestoneJpaRepository milestones, RoadmapStepJpaRepository steps) {
        this.policies=policies;this.templates=templates;this.sets=sets;this.recommendations=recommendations;
        this.evidence=evidence;this.roadmapPolicies=roadmapPolicies;this.roadmaps=roadmaps;this.milestones=milestones;this.steps=steps;
    }

    public RecommendationPolicy loadActivePolicy(UUID profileId) {
        var policy=policies.findByStatus("ACTIVE").orElseThrow();
        Map<RuleCategory,RecommendationTemplate> mapped=new EnumMap<>(RuleCategory.class);
        templates.findAllByPolicyIdAndCareerProfileVersionId(policy.id,profileId).forEach(value->{
            RuleCategory category=RuleCategory.valueOf(value.category);
            mapped.put(category,new RecommendationTemplate(category,RecommendationType.valueOf(value.type),
                value.prerequisiteOrder,value.effortHours,value.title,value.rationaleCode,value.completionCriteria,value.expectedEvidence));
        });
        return new RecommendationPolicy(policy.id,policy.versionLabel,profileId,mapped);
    }
    public RoadmapPolicy loadActiveRoadmapPolicy(UUID recommendationPolicyId,UUID profileId){var value=roadmapPolicies.findByStatusAndRecommendationPolicyId("ACTIVE",recommendationPolicyId).orElseThrow();Map<RuleCategory,Integer> order=new EnumMap<>(RuleCategory.class);templates.findAllByPolicyIdAndCareerProfileVersionId(recommendationPolicyId,profileId).forEach(item->order.put(RuleCategory.valueOf(item.category),item.prerequisiteOrder));return new RoadmapPolicy(value.id,value.versionLabel,value.recommendationPolicyId,order);}
    public void supersedeCurrent(UUID userId,java.time.Instant now){
        var currentSets=sets.findAllByUserIdAndStatus(userId,"PUBLISHED");currentSets.forEach(value->value.status="SUPERSEDED");sets.saveAll(currentSets);
        var activeRoadmaps=roadmaps.findAllByUserIdAndStatusIn(userId,List.of("CREATED","IN_PROGRESS"));activeRoadmaps.forEach(value->{value.status="ARCHIVED";value.updatedAt=now;});roadmaps.saveAll(activeRoadmaps);
    }
    public Optional<RecommendationSet> findSetByBasis(UUID userId,UUID readinessId,UUID policyId){return sets.findByUserIdAndReadinessIdAndPolicyId(userId,readinessId,policyId).map(this::map);}
    public Optional<RecommendationSet> findCurrentSet(UUID userId){return sets.findFirstByUserIdOrderByGeneratedAtDescIdDesc(userId).map(this::map);}
    public Optional<RecommendationSet> findSetByIdAndOwner(UUID setId,UUID userId){return sets.findByIdAndUserId(setId,userId).map(this::map);}
    public Optional<RecommendationSet> findSetByRecommendationIdAndOwner(UUID recommendationId,UUID userId){return sets.findByRecommendationIdAndUserId(recommendationId,userId).map(this::map);}
    public List<RecommendationSet> findSetsByOwner(UUID userId){return sets.findAllByUserIdOrderByGeneratedAtDescIdDesc(userId).stream().map(this::map).toList();}
    public List<RecommendationEvidence> findEvidenceByRecommendationAndOwner(UUID recommendationId,UUID userId){return evidence.findEvidenceByRecommendationAndOwner(recommendationId,userId).stream().map(item->new RecommendationEvidence(item.getEvidenceId(),item.getEvidenceType(),item.getSourceReference(),item.getObservedFactSummary(),item.getConfidence(),item.getCreatedAt())).toList();}
    public RecommendationSet saveSet(RecommendationSet value){
        RecommendationSetJpaEntity set=new RecommendationSetJpaEntity();set.id=value.recommendationSetId();set.userId=value.userId();set.readinessId=value.careerReadinessId();set.policyId=value.policyId();set.status=value.status();set.generatedAt=value.generatedAt();sets.save(set);
        List<RecommendationJpaEntity> items=new ArrayList<>();List<RecommendationEvidenceJpaEntity> links=new ArrayList<>();
        for(Recommendation recommendation:value.recommendations()){RecommendationJpaEntity item=new RecommendationJpaEntity();item.id=recommendation.recommendationId();item.setId=value.recommendationSetId();item.gapId=recommendation.skillGapId();item.category=recommendation.category().name();item.type=recommendation.type().name();item.priority=recommendation.priority().name();item.rationaleCode=recommendation.rationaleCode();item.title=recommendation.title();item.completionCriteria=recommendation.completionCriteria();item.expectedEvidence=recommendation.expectedEvidence();item.effortHours=recommendation.effortHours();item.position=recommendation.position();item.status=recommendation.status();item.updatedAt=value.generatedAt();items.add(item);recommendation.evidenceIds().forEach(id->links.add(new RecommendationEvidenceJpaEntity(recommendation.recommendationId(),id)));}
        recommendations.saveAll(items);evidence.saveAll(links);return value;
    }
    public Optional<LearningRoadmap> findRoadmapByBasis(UUID userId,UUID setId,UUID policyId){return roadmaps.findByUserIdAndSetIdAndPolicyId(userId,setId,policyId).map(this::map);}
    public Optional<LearningRoadmap> findActiveRoadmap(UUID userId){return roadmaps.findFirstByUserIdAndStatusInOrderByGeneratedAtDescIdDesc(userId,List.of("CREATED","IN_PROGRESS")).map(this::map);}
    public Optional<LearningRoadmap> findRoadmapByIdAndOwner(UUID roadmapId,UUID userId){return roadmaps.findByIdAndUserId(roadmapId,userId).map(this::map);}
    public List<LearningRoadmap> findRoadmapsByOwner(UUID userId){return roadmaps.findAllByUserIdOrderByGeneratedAtDescIdDesc(userId).stream().map(this::map).toList();}
    public LearningRoadmap saveRoadmap(LearningRoadmap value){
        LearningRoadmapJpaEntity roadmap=new LearningRoadmapJpaEntity();roadmap.id=value.roadmapId();roadmap.userId=value.userId();roadmap.setId=value.recommendationSetId();roadmap.policyId=value.policyId();roadmap.status=value.status();roadmap.progressPercent=value.progressPercent();roadmap.generatedAt=value.generatedAt();roadmap.updatedAt=value.updatedAt();roadmaps.save(roadmap);
        milestones.saveAll(value.milestones().stream().map(item->{RoadmapMilestoneJpaEntity entity=new RoadmapMilestoneJpaEntity();entity.id=item.milestoneId();entity.roadmapId=value.roadmapId();entity.position=item.position();entity.category=item.category().name();entity.title=item.title();entity.status=item.status();return entity;}).toList());
        steps.saveAll(value.steps().stream().map(item->{RoadmapStepJpaEntity entity=new RoadmapStepJpaEntity();entity.id=item.stepId();entity.roadmapId=value.roadmapId();entity.milestoneId=item.milestoneId();entity.recommendationId=item.recommendationId();entity.position=item.position();entity.category=item.category().name();entity.title=item.title();entity.difficulty=item.difficulty();entity.effortHours=item.effortHours();entity.prerequisiteStepIds=item.prerequisiteStepIds();entity.completionCriteria=item.completionCriteria();entity.expectedEvidence=item.expectedEvidence();entity.status=item.status();entity.updatedAt=value.updatedAt();return entity;}).toList());return value;
    }
    public LearningRoadmap updateRoadmap(LearningRoadmap value){var entity=roadmaps.findByIdAndUserId(value.roadmapId(),value.userId()).orElseThrow();entity.status=value.status();entity.updatedAt=value.updatedAt();roadmaps.save(entity);return map(entity);}
    private RecommendationSet map(RecommendationSetJpaEntity set){var policy=policies.findById(set.policyId).orElseThrow();List<RecommendationJpaEntity> items=recommendations.findAllBySetIdOrderByPosition(set.id);List<UUID> ids=items.stream().map(item->item.id).toList();Map<UUID,List<UUID>> evidenceMap=ids.isEmpty()?Map.of():evidence.findAllByRecommendationIdIn(ids).stream().collect(Collectors.groupingBy(item->item.recommendationId,Collectors.mapping(item->item.evidenceId,Collectors.toList())));return new RecommendationSet(set.id,set.userId,set.readinessId,set.policyId,policy.versionLabel,set.status,items.stream().map(item->new Recommendation(item.id,item.gapId,RuleCategory.valueOf(item.category),RecommendationType.valueOf(item.type),RecommendationPriority.valueOf(item.priority),item.rationaleCode,item.title,item.completionCriteria,item.expectedEvidence,evidenceMap.getOrDefault(item.id,List.of()),item.effortHours,item.position,item.status)).toList(),set.generatedAt);}
    private LearningRoadmap map(LearningRoadmapJpaEntity roadmap){var policy=roadmapPolicies.findById(roadmap.policyId).orElseThrow();return new LearningRoadmap(roadmap.id,roadmap.userId,roadmap.setId,roadmap.policyId,policy.versionLabel,roadmap.status,roadmap.progressPercent,milestones.findAllByRoadmapIdOrderByPosition(roadmap.id).stream().map(item->new RoadmapMilestone(item.id,item.position,RuleCategory.valueOf(item.category),item.title,item.status)).toList(),steps.findAllByRoadmapIdOrderByPosition(roadmap.id).stream().map(item->new RoadmapStep(item.id,item.milestoneId,item.recommendationId,item.position,RuleCategory.valueOf(item.category),item.title,item.difficulty,item.effortHours,item.prerequisiteStepIds,item.completionCriteria,item.expectedEvidence,item.status)).toList(),roadmap.generatedAt,roadmap.updatedAt);}
}
