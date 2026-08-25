package com.devpath.identity.application;

import com.devpath.identity.domain.PreferenceType;
import com.devpath.identity.domain.UserId;
import com.devpath.identity.domain.UserPreference;
import java.util.Optional;

public interface UserPreferenceRepositoryPort {
    Optional<UserPreference> findActive(UserId userId, PreferenceType type);
    UserPreference save(UserPreference preference);
}
