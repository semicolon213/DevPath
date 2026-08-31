package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvidenceView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositoryReviewValidatorTest {
    private final RepositoryReviewValidator validator = new RepositoryReviewValidator(new ObjectMapper());

    @Test
    void acceptsFiveGroundedSectionsAndRejectsUnknownEvidence() {
        UUID evidenceId = UUID.randomUUID();
        var evidence = new RuleEvidenceListView(UUID.randomUUID(), List.of(new RuleEvidenceView(evidenceId,
            "README_PRESENT", "DIRECT", "REPOSITORY_PATH", "README.md", "README is present", BigDecimal.ONE)));
        String sections = """
            {"summary":"근거 기반 검토입니다","sections":[
            {"category":"ARCHITECTURE","review":"확인 가능한 근거가 없습니다","evidenceIds":[]},
            {"category":"TESTING","review":"확인 가능한 근거가 없습니다","evidenceIds":[]},
            {"category":"DEVOPS","review":"확인 가능한 근거가 없습니다","evidenceIds":[]},
            {"category":"DOCUMENTATION","review":"문서 근거가 확인됩니다","evidenceIds":["%s"]},
            {"category":"COLLABORATION","review":"확인 가능한 근거가 없습니다","evidenceIds":[]}]}
            """.formatted(evidenceId);

        assertThat(validator.validate(sections, evidence).passed()).isTrue();
        assertThat(validator.validate(sections.replace(evidenceId.toString(), UUID.randomUUID().toString()), evidence)
            .violations()).contains("UNSUPPORTED_EVIDENCE_REFERENCE");
    }
}
