package com.devpath.identity.application;

public interface ProcessOAuthLoginUseCase {
    AuthenticatedUser process(OAuthLoginCommand command);
}
