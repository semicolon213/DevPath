package com.devpath.ai.application;

import com.devpath.rule.application.RuleCategoryScoreView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvidenceView;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.SkillAssessmentView;
import com.devpath.rule.application.SkillMatrixView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SkillExplanationValidator {
    public static final String VERSION = "skill-explanation-validator-v4";
    private static final int GROUNDED_EVIDENCE_LIMIT = 2;
    private static final Set<String> ROOT_FIELDS = Set.of("summary", "strengths", "improvementAreas");
    private static final Set<String> REQUIRED_ITEM_FIELDS = Set.of("skillKey", "explanation", "evidenceIds");
    private static final Pattern NUMBER = Pattern.compile("\\p{N}");
    private static final Pattern UNSAFE = Pattern.compile("(?i)<|javascript:|on(?:click|error|load)\\s*=");
    private final ObjectMapper mapper;

    public SkillExplanationValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public SkillExplanationValidation validate(
        String raw, SkillMatrixView matrix, RuleEvaluationView evaluation, RuleEvidenceListView evidence
    ) {
        List<String> violations = new ArrayList<>();
        if (raw == null || raw.isBlank() || raw.length() > 20_000) {
            return new SkillExplanationValidation(null, List.of("MALFORMED_OR_EMPTY_RESPONSE"));
        }
        try {
            JsonNode root = mapper.readTree(raw);
            if (!root.isObject() || !ROOT_FIELDS.containsAll(fields(root))
                || root.get("strengths") == null || root.get("improvementAreas") == null) {
                return new SkillExplanationValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            ObjectNode normalized = ((ObjectNode) root).deepCopy();
            if (!normalized.hasNonNull("summary") || normalized.get("summary").asText().isBlank()) {
                normalized.put("summary", "검증된 Rule Engine 근거를 바탕으로 강점과 개선 영역을 정리했습니다.");
            }
            if (!normalizeItems(normalized.get("strengths"))
                || !normalizeItems(normalized.get("improvementAreas"))) {
                return new SkillExplanationValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
            }
            SkillExplanationContent content = mapper.treeToValue(normalized, SkillExplanationContent.class);
            validateText(content.summary(), 2000, violations);
            validateItems(content.strengths(), matrix, SkillAssessmentView::strength, violations);
            validateItems(content.improvementAreas(), matrix, SkillAssessmentView::weakness, violations);
            if (!matrix.skills().isEmpty() && content.strengths().isEmpty() && content.improvementAreas().isEmpty()) {
                violations.add("INCOMPLETE_SKILL_EXPLANATION");
            }
            content = new SkillExplanationContent(content.summary(), complete(content.strengths(), matrix,
                SkillAssessmentView::strength), complete(content.improvementAreas(), matrix, SkillAssessmentView::weakness));
            return new SkillExplanationValidation(new SkillExplanationContent(content.summary(),
                ground(content.strengths(), matrix, evaluation, evidence),
                ground(content.improvementAreas(), matrix, evaluation, evidence)),
                violations.stream().distinct().toList());
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            return new SkillExplanationValidation(null, List.of("INVALID_RESPONSE_SCHEMA"));
        }
    }

    private static boolean normalizeItems(JsonNode items) {
        if (!items.isArray()) return false;
        for (JsonNode item : items) {
            if (!item.isObject() || !fields(item).containsAll(REQUIRED_ITEM_FIELDS)
                || !Set.of("skillKey", "explanation", "evidenceIds", "skillName").containsAll(fields(item))) {
                return false;
            }
            ((ObjectNode) item).remove("skillName");
        }
        return true;
    }

    private static List<SkillExplanationContent.Item> complete(
        List<SkillExplanationContent.Item> items, SkillMatrixView matrix, Predicate<SkillAssessmentView> expectedState
    ) {
        Map<String, SkillExplanationContent.Item> supplied = items.stream()
            .collect(Collectors.toMap(SkillExplanationContent.Item::skillKey, value -> value));
        return matrix.skills().stream().filter(expectedState).map(skill -> supplied.getOrDefault(skill.skillKey(),
            new SkillExplanationContent.Item(skill.skillKey(), fallback(skill), List.of()))).toList();
    }

    private static String fallback(SkillAssessmentView skill) {
        String label = switch (skill.category()) {
            case "LANGUAGE" -> "언어 활용";
            case "FRAMEWORK" -> "프레임워크 활용";
            case "DATABASE" -> "데이터베이스";
            case "ARCHITECTURE" -> "아키텍처";
            case "TESTING" -> "테스트";
            case "DEVOPS" -> "배포 및 운영";
            case "DOCUMENTATION" -> "문서화";
            case "ACTIVITY" -> "개발 활동";
            default -> "기술 역량";
        };
        return skill.strength() ? "Rule Engine 근거에서 " + label + " 강점이 확인되었습니다."
            : "Rule Engine 근거에서 " + label + " 개선 필요성이 확인되었습니다.";
    }

    private static void validateItems(
        List<SkillExplanationContent.Item> items, SkillMatrixView matrix,
        Predicate<SkillAssessmentView> expectedState, List<String> violations
    ) {
        if (items.size() > 20) violations.add("INVALID_RESPONSE_SCHEMA");
        Map<String, SkillAssessmentView> skills = matrix.skills().stream()
            .collect(Collectors.toMap(SkillAssessmentView::skillKey, value -> value));
        Set<String> seen = new HashSet<>();
        for (var item : items) {
            SkillAssessmentView source = item.skillKey() == null ? null : skills.get(item.skillKey());
            if (source == null || !expectedState.test(source) || !seen.add(item.skillKey())) {
                violations.add("UNSUPPORTED_SKILL_CLAIM");
            }
            validateText(item.explanation(), 1000, violations);
        }
    }

    private static List<SkillExplanationContent.Item> ground(
        List<SkillExplanationContent.Item> items, SkillMatrixView matrix,
        RuleEvaluationView evaluation, RuleEvidenceListView evidence
    ) {
        if (!matrix.evaluationId().equals(evaluation.evaluationId())
            || !matrix.evaluationId().equals(evidence.evaluationId())) {
            throw new IllegalArgumentException("Grounding context does not match the Skill Matrix");
        }
        Map<String, SkillAssessmentView> skills = matrix.skills().stream()
            .collect(Collectors.toMap(SkillAssessmentView::skillKey, value -> value));
        Map<String, RuleCategoryScoreView> categories = evaluation.categoryScores().stream()
            .collect(Collectors.toMap(RuleCategoryScoreView::category, value -> value));
        return items.stream().map(item -> {
            SkillAssessmentView source = skills.get(item.skillKey());
            RuleCategoryScoreView category = source == null ? null : categories.get(source.category());
            Map<String, String> statuses = category == null ? Map.of() : category.ruleResults().stream()
                .collect(Collectors.toMap(rule -> rule.ruleId(), rule -> rule.status()));
            Set<String> relevant = source != null && source.strength()
                ? Set.of("PASSED") : Set.of("PARTIAL", "FAILED");
            Set<UUID> relevantEvidenceIds = evidence.evidence().stream()
                .filter(value -> statuses.containsKey(value.ruleId())
                    && relevant.contains(statuses.get(value.ruleId())))
                .map(RuleEvidenceView::evidenceId).collect(Collectors.toSet());
            List<UUID> evidenceIds = source == null ? List.of() : source.evidenceIds().stream()
                .filter(relevantEvidenceIds::contains).limit(GROUNDED_EVIDENCE_LIMIT).toList();
            return new SkillExplanationContent.Item(item.skillKey(),
                item.explanation() + groundedAdvice(source, statuses), evidenceIds);
        }).toList();
    }

    private static String groundedAdvice(SkillAssessmentView source, Map<String, String> statuses) {
        if (source == null || !source.weakness()) return "";
        return statuses.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .filter(entry -> Set.of("PARTIAL", "FAILED").contains(entry.getValue()))
            .map(entry -> advice(entry.getKey())).filter(value -> !value.isEmpty()).distinct()
            .map(value -> " " + value).collect(Collectors.joining());
    }

    private static String advice(String ruleId) {
        return switch (ruleId) {
            case "COMMIT_COUNT" -> "조언: 의미 있는 변경을 커밋으로 나누어 기록하세요. 확인 기준: 다음 저장소 동기화에서 추가 커밋 이력이 수집됩니다.";
            case "CONTRIBUTOR_COUNT" -> "조언: 협업 변경의 작성자 이력을 보존하세요. 확인 기준: 다음 저장소 동기화에서 기여자 이력이 수집됩니다.";
            case "BRANCH_COUNT" -> "조언: 독립 작업은 브랜치 이력으로 남기세요. 확인 기준: 다음 저장소 동기화에서 브랜치 이력이 수집됩니다.";
            case "CI_WORKFLOWS", "CI_WORKFLOW_METADATA" -> "조언: 변경 검증을 실행하는 CI 워크플로 설정 파일을 저장소에 추가하세요. 확인 기준: 다음 분석에서 워크플로 설정이 감지됩니다.";
            case "CONTAINER_CONFIGURATION" -> "조언: 재현 가능한 실행을 위한 컨테이너 설정을 저장소에 추가하세요. 확인 기준: 다음 분석에서 컨테이너 설정이 감지됩니다.";
            case "INFRASTRUCTURE_AS_CODE" -> "조언: 인프라 구성을 코드 자산으로 저장소에 기록하세요. 확인 기준: 다음 분석에서 인프라 코드가 감지됩니다.";
            case "DEPLOYMENT_CONFIGURATION" -> "조언: 배포 절차를 실행 가능한 설정으로 저장소에 기록하세요. 확인 기준: 다음 분석에서 배포 설정이 감지됩니다.";
            case "TEST_FILES" -> "조언: 변경 동작을 검증하는 자동화 테스트 파일을 추가하세요. 확인 기준: 다음 분석에서 테스트 파일이 감지됩니다.";
            case "TEST_FRAMEWORKS" -> "조언: 저장소의 테스트 실행 구성을 명시하세요. 확인 기준: 다음 분석에서 테스트 프레임워크 근거가 감지됩니다.";
            default -> "";
        };
    }

    private static void validateText(String value, int maxLength, List<String> violations) {
        if (value == null || value.isBlank() || value.length() > maxLength) violations.add("INVALID_RESPONSE_SCHEMA");
        else {
            if (NUMBER.matcher(value).find()) violations.add("UNTRACEABLE_NUMERIC_CLAIM");
            if (UNSAFE.matcher(value).find()) violations.add("UNSAFE_RENDERED_CONTENT");
        }
    }

    private static Set<String> fields(JsonNode node) {
        Set<String> values = new HashSet<>();
        node.fieldNames().forEachRemaining(values::add);
        return values;
    }
}
