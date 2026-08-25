package com.devpath.career.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.career.application.CareerCatalogPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = "devpath.runtime.worker-enabled=false")
@EnabledIfEnvironmentVariable(named = "DEVPATH_DB_URL", matches = ".+")
class CareerCatalogPersistenceIntegrationTest {
    @Autowired CareerCatalogPort catalog;

    @Test
    @Transactional(readOnly = true)
    void loadsTheVersionedCareerCatalogFromPostgresql() {
        assertThat(catalog.findSupported()).hasSize(9);
        assertThat(catalog.findSupportedById("backend")).get().satisfies(profile -> {
            assertThat(profile.profileVersion()).isEqualTo("career-v2");
            assertThat(profile.requiredCompetencies()).contains("API 설계", "테스트");
            assertThat(profile.priorityWeights()).containsEntry("TESTING", "20");
        });
        assertThat(catalog.findSupportedById("full-stack")).isEmpty();
    }
}
