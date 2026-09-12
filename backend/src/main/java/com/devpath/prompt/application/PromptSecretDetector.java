package com.devpath.prompt.application;

import java.util.regex.Pattern;

final class PromptSecretDetector {
    private static final Pattern SECRET = Pattern.compile(
        "(-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|\\bgh[pousr]_[A-Za-z0-9]{30,}\\b|"
            + "\\bgithub_pat_[A-Za-z0-9_]{40,}\\b|\\bsk-(?:proj-)?[A-Za-z0-9_-]{20,}\\b|"
            + "\\bAKIA[0-9A-Z]{16}\\b|(?i:password\\s*[:=]))"
    );

    private PromptSecretDetector() {}

    static void reject(String context) {
        if (SECRET.matcher(context).find()) throw new IllegalArgumentException("Prompt context contains sensitive data");
    }
}
