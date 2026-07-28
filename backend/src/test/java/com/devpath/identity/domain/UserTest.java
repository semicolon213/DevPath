package com.devpath.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {
    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Test
    void registersAnActiveUserWithNormalizedProfileData() {
        User user = User.register(" DevPath User ", " https://avatars.example/user ", NOW);

        assertThat(user.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.displayName()).isEqualTo("DevPath User");
        assertThat(user.avatarUrl()).isEqualTo("https://avatars.example/user");
        assertThat(user.createdAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsAuthenticationForSuspendedAccounts() {
        User user = User.rehydrate(
            UserId.newId(),
            AccountStatus.SUSPENDED,
            "DevPath User",
            null,
            NOW,
            NOW,
            1
        );

        assertThatThrownBy(user::assertAuthenticationAllowed)
            .isInstanceOf(DisabledAccountException.class);
    }
}
