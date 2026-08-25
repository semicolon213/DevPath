package com.devpath.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.devpath.identity.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserProfileApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void rejectsTargetsOutsideTheVersionedCatalog() {
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        UserPreferenceRepositoryPort preferences = mock(UserPreferenceRepositoryPort.class);
        TargetCatalogPort catalog = mock(TargetCatalogPort.class);
        UserId userId = UserId.newId();
        when(users.findById(userId)).thenReturn(Optional.of(User.register("User", null, NOW)));
        when(catalog.supports(PreferenceType.COMPANY, "meta")).thenReturn(false);
        var service = service(users, mock(UserProfileRepositoryPort.class), preferences, catalog);

        assertThatThrownBy(() -> service.setCompany(userId, "meta")).isInstanceOf(UnsupportedTargetException.class);
        verify(preferences, never()).save(any());
    }

    @Test
    void storesCatalogVersionAndReturnsTheSelectedCareer() {
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        UserPreferenceRepositoryPort preferences = mock(UserPreferenceRepositoryPort.class);
        TargetCatalogPort catalog = mock(TargetCatalogPort.class);
        UserId userId = UserId.newId();
        when(users.findById(userId)).thenReturn(Optional.of(User.register("User", null, NOW)));
        when(catalog.supports(PreferenceType.CAREER, "backend")).thenReturn(true);
        when(catalog.version()).thenReturn("v1");
        when(preferences.findActive(userId, PreferenceType.CAREER)).thenReturn(Optional.empty());
        when(preferences.findActive(userId, PreferenceType.COMPANY)).thenReturn(Optional.empty());
        when(preferences.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(users, mock(UserProfileRepositoryPort.class), preferences, catalog);

        service.setCareer(userId, "backend");

        var captor = org.mockito.ArgumentCaptor.forClass(UserPreference.class);
        verify(preferences).save(captor.capture());
        assertThat(captor.getValue().catalogVersion()).isEqualTo("v1");
    }

    private UserProfileApplicationService service(UserRepositoryPort users, UserProfileRepositoryPort profiles,
        UserPreferenceRepositoryPort preferences, TargetCatalogPort catalog) {
        return new UserProfileApplicationService(users, profiles, preferences, Clock.fixed(NOW, ZoneOffset.UTC), catalog);
    }
}
