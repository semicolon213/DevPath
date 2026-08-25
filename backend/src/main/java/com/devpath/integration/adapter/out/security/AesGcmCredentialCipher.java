package com.devpath.integration.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class AesGcmCredentialCipher {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final SecretKey key;
    private final String keyVersion;
    private final SecureRandom secureRandom;

    public AesGcmCredentialCipher(SecretKey key, String keyVersion) {
        this(key, keyVersion, new SecureRandom());
    }

    AesGcmCredentialCipher(SecretKey key, String keyVersion, SecureRandom secureRandom) {
        this.key = Objects.requireNonNull(key);
        this.keyVersion = requireText(keyVersion, "Key version is required");
        this.secureRandom = Objects.requireNonNull(secureRandom);
        int keyBits = key.getEncoded().length * Byte.SIZE;
        if (keyBits != 128 && keyBits != 192 && keyBits != 256) {
            throw new IllegalArgumentException("AES key must contain 128, 192, or 256 bits");
        }
    }

    public EncryptedSecret encrypt(String plaintext, String context) {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        return new EncryptedSecret(transform(Cipher.ENCRYPT_MODE, plaintext.getBytes(StandardCharsets.UTF_8), iv, context), iv, keyVersion);
    }

    public String decrypt(EncryptedSecret encrypted, String context) {
        if (!keyVersion.equals(encrypted.keyVersion())) {
            throw new IllegalArgumentException("Credential key version is not available");
        }
        return new String(transform(Cipher.DECRYPT_MODE, encrypted.ciphertext(), encrypted.iv(), context), StandardCharsets.UTF_8);
    }

    private byte[] transform(int mode, byte[] input, byte[] iv, String context) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(requireText(context, "Encryption context is required").getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Provider credential encryption operation failed", exception);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
