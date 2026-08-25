package com.devpath.integration.adapter.out.security;

public record EncryptedSecret(byte[] ciphertext, byte[] iv, String keyVersion) {
    public EncryptedSecret {
        ciphertext = ciphertext.clone();
        iv = iv.clone();
    }

    @Override
    public byte[] ciphertext() {
        return ciphertext.clone();
    }

    @Override
    public byte[] iv() {
        return iv.clone();
    }
}
