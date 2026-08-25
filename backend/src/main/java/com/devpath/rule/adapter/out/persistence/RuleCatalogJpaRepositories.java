package com.devpath.rule.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RuleSetJpaRepository extends JpaRepository<RuleSetJpaEntity, UUID> {
    Optional<RuleSetJpaEntity> findByScopeAndStatus(String scope, String status);
}

interface RuleSetVersionJpaRepository extends JpaRepository<RuleSetVersionJpaEntity, UUID> {}

interface RuleCategoryWeightJpaRepository extends JpaRepository<RuleCategoryWeightJpaEntity, RuleCategoryWeightId> {
    List<RuleCategoryWeightJpaEntity> findAllByRuleSetVersionId(UUID ruleSetVersionId);
}

interface RuleDefinitionJpaRepository extends JpaRepository<RuleDefinitionJpaEntity, RuleDefinitionId> {
    List<RuleDefinitionJpaEntity> findAllByRuleSetVersionIdOrderByPriorityAscRuleIdAsc(UUID ruleSetVersionId);
}
