package com.devpath.identity.application;

import com.devpath.identity.domain.User;
import com.devpath.identity.domain.UserId;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(UserId userId);

    User save(User user);
}
