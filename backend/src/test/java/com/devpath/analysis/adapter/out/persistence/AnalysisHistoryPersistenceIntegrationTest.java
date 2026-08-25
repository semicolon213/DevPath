package com.devpath.analysis.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = "devpath.runtime.worker-enabled=false")
@EnabledIfEnvironmentVariable(named = "DEVPATH_DB_URL", matches = ".+")
class AnalysisHistoryPersistenceIntegrationTest {
    @Autowired AnalysisResultJpaRepository results;

    @Test
    @Transactional(readOnly = true)
    void ownerScopedHistoryJoinExecutesAgainstTheAuthoritativeSchema() {
        UUID absentOwner = UUID.randomUUID();

        assertThat(results.countByUserId(absentOwner)).isZero();
        assertThat(results.findHistoryByOwner(absentOwner, PageRequest.of(0, 20))).isEmpty();
        assertThat(results.findHistoryByOwnerAndRepository(absentOwner, UUID.randomUUID(), PageRequest.of(0, 20)))
            .isEmpty();
        assertThat(results.findReusableResult(absentOwner, UUID.randomUUID(), "REPOSITORY_BASELINE",
            PageRequest.of(0, 1))).isEmpty();
        assertThat(results.findFirstByUserIdAndRepositoryIdOrderByCompletedAtDescIdDesc(
            absentOwner, UUID.randomUUID())).isEmpty();
    }
}
