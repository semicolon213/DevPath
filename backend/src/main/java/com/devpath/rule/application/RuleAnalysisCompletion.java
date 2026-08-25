package com.devpath.rule.application;

import com.devpath.rule.domain.CompletedRuleEvaluation;
import com.devpath.rule.domain.SkillMatrix;

public record RuleAnalysisCompletion(CompletedRuleEvaluation evaluation, SkillMatrix skillMatrix) {}
