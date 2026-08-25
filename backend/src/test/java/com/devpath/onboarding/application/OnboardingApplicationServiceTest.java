package com.devpath.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.analysis.application.AnalysisPersistencePort;
import com.devpath.identity.application.UserPreferenceView;
import com.devpath.identity.application.UserProfileApplicationService;
import com.devpath.identity.application.UserProfileView;
import com.devpath.identity.domain.UserId;
import com.devpath.integration.application.ConnectedAccountView;
import com.devpath.integration.application.ProviderCredentialSummaryPort;
import com.devpath.repository.application.RepositoryPersistencePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingApplicationServiceTest {
    private static final UUID USER_ID = UUID.fromString("124393a2-4718-4475-bbe4-198e8af67cee");
    private static final Instant NOW = Instant.parse("2026-08-25T06:00:00Z");

    @Test
    void derivesTheNextStepFromOwnerScopedPersistedResourcesAndAuditsTheView() {
        var fixture = new Fixture();
        fixture.base(new UserPreferenceView("backend", null, NOW));
        when(fixture.connections.findByUserId(USER_ID)).thenReturn(List.of(new ConnectedAccountView(
            UUID.randomUUID(), "GITHUB", "ACTIVE", List.of("contents:read"), NOW, null)));
        when(fixture.repositories.countByOwner(USER_ID, false)).thenReturn(1L);
        when(fixture.repositories.countByOwnerAndSyncStatus(USER_ID, "SYNCHRONIZED", false)).thenReturn(0L);
        when(fixture.analyses.countHistoryByOwner(USER_ID)).thenReturn(0L);

        var result = fixture.service().getProgress(USER_ID);

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(result.nextStep()).isEqualTo("INITIAL_SYNC");
        assertThat(result.completedStepCount()).isEqualTo(5);
        assertThat(result.steps()).filteredOn(step -> step.step().equals("COMPANY_TARGET"))
            .singleElement().satisfies(step -> {
                assertThat(step.requirement()).isEqualTo("OPTIONAL");
                assertThat(step.status()).isEqualTo("INCOMPLETE");
            });
        verify(fixture.audit).record(OnboardingAuditEvent.ONBOARDING_PROGRESS_VIEWED, USER_ID, NOW);
    }

    @Test
    void marksTheDashboardReadyWithoutRequiringTheOptionalCompanyTarget() {
        var fixture = new Fixture();
        fixture.base(new UserPreferenceView("backend", null, NOW));
        when(fixture.connections.findByUserId(USER_ID)).thenReturn(List.of(new ConnectedAccountView(
            UUID.randomUUID(), "GITHUB", "ACTIVE", List.of(), NOW, null)));
        when(fixture.repositories.countByOwner(USER_ID, false)).thenReturn(1L);
        when(fixture.repositories.countByOwnerAndSyncStatus(USER_ID, "SYNCHRONIZED", false)).thenReturn(1L);
        when(fixture.analyses.countHistoryByOwner(USER_ID)).thenReturn(1L);

        var result = fixture.service().getProgress(USER_ID);

        assertThat(result.status()).isEqualTo("DASHBOARD_READY");
        assertThat(result.nextStep()).isEqualTo("DASHBOARD_READY");
        assertThat(result.completedStepCount()).isEqualTo(7);
    }

    private static final class Fixture {
        private final UserProfileApplicationService profiles = mock(UserProfileApplicationService.class);
        private final ProviderCredentialSummaryPort connections = mock(ProviderCredentialSummaryPort.class);
        private final RepositoryPersistencePort repositories = mock(RepositoryPersistencePort.class);
        private final AnalysisPersistencePort analyses = mock(AnalysisPersistencePort.class);
        private final OnboardingAuditPort audit = mock(OnboardingAuditPort.class);

        void base(UserPreferenceView preferences) {
            when(profiles.getProfile(new UserId(USER_ID))).thenReturn(new UserProfileView(
                UUID.randomUUID(), "Developer", null, null, NOW));
            when(profiles.getPreferences(new UserId(USER_ID))).thenReturn(preferences);
        }

        OnboardingApplicationService service() {
            return new OnboardingApplicationService(profiles, connections, repositories, analyses, audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }
}
