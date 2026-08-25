package com.devpath.identity.adapter.in.security;

import com.devpath.identity.application.AuthenticatedUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DevPathOAuth2User implements OAuth2User, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final String displayName;
    private final String avatarUrl;
    private final Map<String, Object> attributes;
    private final List<GrantedAuthority> authorities;

    public DevPathOAuth2User(AuthenticatedUser user, Map<String, Object> attributes) {
        this.userId = user.userId();
        this.displayName = user.displayName();
        this.avatarUrl = user.avatarUrl();
        this.attributes = copyNonNullAttributes(attributes);
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private Map<String, Object> copyNonNullAttributes(Map<String, Object> attributes) {
        var nonNullAttributes = new LinkedHashMap<String, Object>();
        attributes.forEach((key, value) -> {
            if (key != null && value != null) {
                nonNullAttributes.put(key, value);
            }
        });
        return java.util.Collections.unmodifiableMap(nonNullAttributes);
    }

    public UUID userId() {
        return userId;
    }

    public String displayName() {
        return displayName;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
