package com.devpath.identity.application;

import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.User;
import com.devpath.identity.domain.UserId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserApplicationService {
    private final UserRepositoryPort userRepository;

    public CurrentUserApplicationService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser find(UserId userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.assertAuthenticationAllowed();
        return AuthenticatedUser.from(user, OAuthProvider.GITHUB);
    }
}
