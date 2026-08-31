package com.devpath.ai.application;

public class GenerationProviderException extends RuntimeException {
    private final String code;

    public GenerationProviderException(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
