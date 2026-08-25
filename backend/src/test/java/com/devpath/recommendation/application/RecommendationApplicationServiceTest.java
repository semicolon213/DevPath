package com.devpath.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.devpath.career.domain.CareerReadiness;
import com.devpath.career.domain.CareerReadinessStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationApplicationServiceTest {
    @Test
    void doesNotInventRecommendationsForInsufficientReadiness() {
        RecommendationPersistencePort persistence=mock(RecommendationPersistencePort.class);
        var service=new RecommendationApplicationService(persistence);
        var readiness=new CareerReadiness(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"backend",
            UUID.randomUUID(),"career-v2",UUID.randomUUID(),"readiness-v1","baseline-v2",
            CareerReadinessStatus.INSUFFICIENT_EVIDENCE,null,null,new BigDecimal("50"),List.of("DATABASE"),List.of(),
            Instant.parse("2026-08-25T00:00:00Z"));

        assertThat(service.generate(readiness,Instant.parse("2026-08-25T00:00:01Z"))).isEmpty();
        verifyNoInteractions(persistence);
    }
}
