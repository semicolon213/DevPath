package com.devpath.identity.application;

import com.devpath.identity.domain.UserId;
import com.devpath.identity.domain.UserProfile;
import java.util.Optional;

public interface UserProfileRepositoryPort {
    Optional<UserProfile> findByUserId(UserId userId);
    UserProfile save(UserProfile profile);
}
