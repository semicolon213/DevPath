package com.devpath.analysis.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker=true)
class AnalysisHistoryPersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry){registry.add("spring.datasource.url",POSTGRES::getJdbcUrl);registry.add("spring.datasource.username",POSTGRES::getUsername);registry.add("spring.datasource.password",POSTGRES::getPassword);}
    @Autowired AnalysisResultJpaRepository results;

    @Test
    @Transactional(readOnly = true)
    void ownerScopedHistoryJoinExecutesAgainstTheAuthoritativeSchema() {
        UUID absentOwner = UUID.randomUUID();

        assertThat(results.countByUserId(absentOwner)).isZero();
        assertThat(results.findHistoryByOwner(absentOwner, PageRequest.of(0, 20))).isEmpty();
        assertThat(results.findHistoryByOwnerAndRepository(absentOwner, UUID.randomUUID(), PageRequest.of(0, 20)))
            .isEmpty();
        assertThat(results.findHistoryByOwnerAndIds(absentOwner,List.of(UUID.randomUUID(),UUID.randomUUID())))
            .isEmpty();
        assertThat(results.findReusableResult(absentOwner, UUID.randomUUID(), "REPOSITORY_BASELINE",
            PageRequest.of(0, 1))).isEmpty();
        assertThat(results.findFirstByUserIdAndRepositoryIdOrderByCompletedAtDescIdDesc(
            absentOwner, UUID.randomUUID())).isEmpty();
    }
}
