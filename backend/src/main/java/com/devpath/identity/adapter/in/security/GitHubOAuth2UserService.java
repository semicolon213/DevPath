package com.devpath.identity.adapter.in.security;

import com.devpath.identity.application.OAuthLoginCommand;
import com.devpath.identity.application.OAuthLoginApplicationService;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.ProviderSubject;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final OAuthLoginApplicationService processOAuthLogin;

    public GitHubOAuth2UserService(OAuthLoginApplicationService processOAuthLogin) {
        this.processOAuthLogin = processOAuthLogin;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User providerUser = delegate.loadUser(userRequest);
        Map<String, Object> attributes = providerUser.getAttributes();

        String subject = requiredAttribute(attributes, "id");
        String username = requiredAttribute(attributes, "login");
        String displayName = optionalAttribute(attributes, "name");
        String avatarUrl = optionalAttribute(attributes, "avatar_url");

        var authenticatedUser = processOAuthLogin.process(new OAuthLoginCommand(
            OAuthProvider.GITHUB,
            new ProviderSubject(subject),
            username,
            displayName == null ? username : displayName,
            avatarUrl
        ));
        return new DevPathOAuth2User(authenticatedUser, attributes);
    }

    private String requiredAttribute(Map<String, Object> attributes, String name) {
        String value = optionalAttribute(attributes, name);
        if (value == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info"),
                "GitHub user information is missing required attribute: " + name
            );
        }
        return value;
    }

    private String optionalAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().trim();
    }
}
