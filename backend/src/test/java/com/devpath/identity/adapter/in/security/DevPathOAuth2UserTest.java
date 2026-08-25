package com.devpath.identity.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.devpath.identity.application.AuthenticatedUser;
import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.OAuthProvider;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DevPathOAuth2UserTest {
    @Test
    void acceptsNullableGitHubAttributesAndRemainsSessionSerializable() throws Exception {
        var attributes = new HashMap<String, Object>();
        attributes.put("id", 1849102);
        attributes.put("login", "devpath-user");
        attributes.put("name", null);
        attributes.put("email", null);

        var principal = new DevPathOAuth2User(authenticatedUser(), attributes);

        assertThat(principal.getAttributes())
            .containsEntry("id", 1849102)
            .containsEntry("login", "devpath-user")
            .doesNotContainKeys("name", "email");

        try (var bytes = new ByteArrayOutputStream();
             var output = new ObjectOutputStream(bytes)) {
            output.writeObject(principal);
            assertThat(bytes.size()).isPositive();
        }
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
            UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97"),
            "DevPath User",
            null,
            AccountStatus.ACTIVE,
            OAuthProvider.GITHUB,
            Instant.parse("2026-07-27T00:00:00Z")
        );
    }
}
