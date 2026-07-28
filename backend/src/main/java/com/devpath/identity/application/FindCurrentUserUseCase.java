package com.devpath.identity.application;

import com.devpath.identity.domain.UserId;

public interface FindCurrentUserUseCase {
    AuthenticatedUser find(UserId userId);
}
