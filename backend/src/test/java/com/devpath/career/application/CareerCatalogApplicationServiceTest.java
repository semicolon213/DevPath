package com.devpath.career.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devpath.career.domain.CareerProfile;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareerCatalogApplicationServiceTest {
    @Test
    void returnsOnlyPersistedSupportedProfilesWithoutCalculatingReadiness() {
        CareerProfile backend = profile("backend", "백엔드 엔지니어");
        var service = new CareerCatalogApplicationService(new CareerCatalogPort() {
            public List<CareerProfile> findSupported() { return List.of(backend); }
            public Optional<CareerProfile> findSupportedById(String id) {
                return "backend".equals(id) ? Optional.of(backend) : Optional.empty();
            }
        });

        assertThat(service.list().careers()).singleElement().satisfies(value -> {
            assertThat(value.careerId()).isEqualTo("backend");
            assertThat(value.profileVersion()).isEqualTo("career-v1");
        });
        assertThat(service.get("backend").requiredCompetencies()).containsExactly("API 설계", "테스트");
        assertThat(service.supports("backend")).isTrue();
        assertThatThrownBy(() -> service.get("unsupported")).isInstanceOf(CareerNotFoundException.class);
    }

    private CareerProfile profile(String id, String localizedName) {
        return new CareerProfile(id, "Backend Engineer", localizedName, CareerProfile.CareerStatus.SUPPORTED,
            UUID.randomUUID(), "career-v1", "서버 측 서비스를 구현합니다.", List.of("Java", "Spring Boot"),
            List.of("API 설계", "테스트"), List.of("CI/CD"), List.of("LANGUAGE", "TESTING"),
            Map.of("TESTING", "HIGH"), List.of("언어", "프레임워크", "테스트"), Instant.parse("2026-08-12T00:00:00Z"));
    }
}
