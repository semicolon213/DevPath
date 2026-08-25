package com.devpath.identity.application;

import com.devpath.identity.domain.CareerStage;
import com.devpath.identity.domain.PreferenceType;
import com.devpath.identity.domain.UserId;
import com.devpath.identity.domain.UserPreference;
import com.devpath.identity.domain.UserProfile;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileApplicationService {
    private final UserRepositoryPort users;
    private final UserProfileRepositoryPort profiles;
    private final UserPreferenceRepositoryPort preferences;
    private final Clock clock;
    private final TargetCatalogPort targetCatalog;

    public UserProfileApplicationService(UserRepositoryPort users, UserProfileRepositoryPort profiles,
                                         UserPreferenceRepositoryPort preferences, Clock clock, TargetCatalogPort targetCatalog) {
        this.users = users;
        this.profiles = profiles;
        this.preferences = preferences;
        this.clock = clock;
        this.targetCatalog = targetCatalog;
    }

    @Transactional(readOnly = true)
    public UserProfileView getProfile(UserId userId) {
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var profile = profiles.findByUserId(userId).orElseThrow(ProfileNotFoundException::new);
        return new UserProfileView(profile.id(), user.displayName(), profile.careerStage(), profile.bio(), profile.updatedAt());
    }

    @Transactional
    public UserProfileView updateProfile(UserId userId, String displayName, CareerStage stage, String bio) {
        Instant now = clock.instant();
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var profile = profiles.findByUserId(userId).orElseThrow(ProfileNotFoundException::new);
        user.updateDisplayName(displayName, now);
        profile.update(stage, bio, now);
        users.save(user);
        profiles.save(profile);
        return new UserProfileView(profile.id(), user.displayName(), profile.careerStage(), profile.bio(), profile.updatedAt());
    }

    @Transactional(readOnly = true)
    public UserPreferenceView getPreferences(UserId userId) {
        users.findById(userId).orElseThrow(UserNotFoundException::new);
        return view(userId);
    }

    @Transactional
    public UserPreferenceView setCareer(UserId userId, String careerId) {
        return setTarget(userId, PreferenceType.CAREER, careerId, "career");
    }

    @Transactional
    public UserPreferenceView setCompany(UserId userId, String companyId) {
        return setTarget(userId, PreferenceType.COMPANY, companyId, "company");
    }

    private UserPreferenceView setTarget(UserId userId, PreferenceType type, String value, String label) {
        users.findById(userId).orElseThrow(UserNotFoundException::new);
        if (!targetCatalog.supports(type, value)) throw new UnsupportedTargetException(label);
        var current = preferences.findActive(userId, type);
        if (current.isPresent() && current.get().selectedValue().equals(value)) return view(userId);
        Instant now = clock.instant();
        current.ifPresent(existing -> { existing.supersede(now); preferences.save(existing); });
        preferences.save(UserPreference.select(userId, type, value, targetCatalog.version(), now));
        return view(userId);
    }

    private UserPreferenceView view(UserId userId) {
        var career = preferences.findActive(userId, PreferenceType.CAREER);
        var company = preferences.findActive(userId, PreferenceType.COMPANY);
        Instant updated = java.util.stream.Stream.of(career, company)
            .flatMap(java.util.Optional::stream).map(UserPreference::selectedAt).max(Instant::compareTo).orElse(null);
        return new UserPreferenceView(career.map(UserPreference::selectedValue).orElse(null),
            company.map(UserPreference::selectedValue).orElse(null), updated);
    }
}
