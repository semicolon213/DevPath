package com.devpath.integration.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class AesGcmCredentialCipherTest {
    private final AesGcmCredentialCipher cipher = new AesGcmCredentialCipher(
        new SecretKeySpec(new byte[32], "AES"),
        "local-v1"
    );

    @Test
    void encryptsAndDecryptsWithoutRetainingPlaintext() {
        EncryptedSecret encrypted = cipher.encrypt("github-secret-token", "user-1:GITHUB");

        assertThat(new String(encrypted.ciphertext(), java.nio.charset.StandardCharsets.UTF_8))
            .doesNotContain("github-secret-token");
        assertThat(cipher.decrypt(encrypted, "user-1:GITHUB")).isEqualTo("github-secret-token");
    }

    @Test
    void rejectsASecretBoundToAnotherOwnerContext() {
        EncryptedSecret encrypted = cipher.encrypt("github-secret-token", "user-1:GITHUB");

        assertThatThrownBy(() -> cipher.decrypt(encrypted, "user-2:GITHUB"))
            .isInstanceOf(IllegalStateException.class);
    }
}
