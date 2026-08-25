package com.devpath.onboarding.application;

import com.devpath.analysis.application.AnalysisPersistencePort;
import com.devpath.identity.application.UserProfileApplicationService;
import com.devpath.identity.domain.UserId;
import com.devpath.integration.application.ProviderCredentialSummaryPort;
import com.devpath.repository.application.RepositoryPersistencePort;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationService {
    private final UserProfileApplicationService profiles;
    private final ProviderCredentialSummaryPort connections;
    private final RepositoryPersistencePort repositories;
    private final AnalysisPersistencePort analyses;
    private final OnboardingAuditPort audit;
    private final Clock clock;

    public OnboardingApplicationService(
        UserProfileApplicationService profiles,
        ProviderCredentialSummaryPort connections,
        RepositoryPersistencePort repositories,
        AnalysisPersistencePort analyses,
        OnboardingAuditPort audit,
        Clock clock
    ) {
        this.profiles = profiles;
        this.connections = connections;
        this.repositories = repositories;
        this.analyses = analyses;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OnboardingProgressView getProgress(UUID userId) {
        var identity = new UserId(userId);
        var profile = profiles.getProfile(identity);
        var preferences = profiles.getPreferences(identity);
        boolean githubConnected = connections.findByUserId(userId).stream()
            .anyMatch(connection -> "GITHUB".equals(connection.provider()) && "ACTIVE".equals(connection.status())
                && (connection.expiresAt() == null || connection.expiresAt().isAfter(clock.instant())));
        long repositoryCount = repositories.countByOwner(userId, false);
        long synchronizedCount = repositories.countByOwnerAndSyncStatus(userId, "SYNCHRONIZED", false);
        long analysisCount = analyses.countHistoryByOwner(userId);

        List<OnboardingProgressView.Step> steps = new ArrayList<>();
        steps.add(step("ACCOUNT", "REQUIRED", true, userId.toString(), "/"));
        steps.add(step("PROFILE", "REQUIRED", true, profile.profileId().toString(), "/onboarding#profile"));
        steps.add(step("CAREER_TARGET", "RECOMMENDED", preferences.careerId() != null,
            preferences.careerId(), "/onboarding#targets"));
        steps.add(step("COMPANY_TARGET", "OPTIONAL", preferences.companyId() != null,
            preferences.companyId(), "/onboarding#targets"));
        steps.add(step("GITHUB_CONNECTION", "REQUIRED", githubConnected, null, "/onboarding#github"));
        steps.add(step("REPOSITORY_IMPORT", "REQUIRED", repositoryCount > 0, null, "/onboarding#github"));
        steps.add(step("INITIAL_SYNC", "REQUIRED", synchronizedCount > 0, null, "/repositories"));
        steps.add(step("INITIAL_ANALYSIS", "RECOMMENDED", analysisCount > 0, null, "/repositories"));

        int completed = (int) steps.stream().filter(value -> "COMPLETE".equals(value.status())).count();
        String nextStep = steps.stream()
            .filter(value -> !"COMPLETE".equals(value.status()) && !"OPTIONAL".equals(value.requirement()))
            .map(OnboardingProgressView.Step::step)
            .findFirst().orElse("DASHBOARD_READY");
        String status = analysisCount > 0 ? "DASHBOARD_READY"
            : completed > 2 ? "IN_PROGRESS" : "GETTING_STARTED";
        var now = clock.instant();
        audit.record(OnboardingAuditEvent.ONBOARDING_PROGRESS_VIEWED, userId, now);
        return new OnboardingProgressView(status, completed, steps.size(), nextStep, steps, now);
    }

    private OnboardingProgressView.Step step(
        String name, String requirement, boolean complete, String resourceId, String actionPath
    ) {
        return new OnboardingProgressView.Step(
            name, requirement, complete ? "COMPLETE" : "INCOMPLETE", resourceId, actionPath);
    }
}
