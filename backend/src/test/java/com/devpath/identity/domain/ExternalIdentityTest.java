package com.devpath.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExternalIdentityTest {
    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Test
    void linksAStableProviderSubjectToAnInternalUser() {
        UserId userId = UserId.newId();

        ExternalIdentity identity = ExternalIdentity.link(
            userId,
            OAuthProvider.GITHUB,
            new ProviderSubject(" 1849102 "),
            "devpath-user",
            "DevPath User",
            null,
            NOW
        );

        assertThat(identity.userId()).isEqualTo(userId);
        assertThat(identity.providerSubject().value()).isEqualTo("1849102");
        assertThat(identity.provider()).isEqualTo(OAuthProvider.GITHUB);
    }

    @Test
    void rejectsMissingAndOversizedProviderSubjects() {
        assertThatThrownBy(() -> new ProviderSubject(" "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProviderSubject("x".repeat(256)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
