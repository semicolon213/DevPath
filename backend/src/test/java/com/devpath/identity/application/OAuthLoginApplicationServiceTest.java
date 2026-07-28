package com.devpath.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.DisabledAccountException;
import com.devpath.identity.domain.ExternalIdentity;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.ProviderSubject;
import com.devpath.identity.domain.User;
import com.devpath.identity.domain.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

class OAuthLoginApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Test
    void provisionsOnceAndReusesTheStableProviderIdentity() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryExternalIdentityRepository identities = new InMemoryExternalIdentityRepository();
        RecordingAudit audit = new RecordingAudit();
        TransactionOperations transactions = immediateTransactions();
        var service = new OAuthLoginApplicationService(
            users,
            identities,
            audit,
            transactions,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
        OAuthLoginCommand command = new OAuthLoginCommand(
            OAuthProvider.GITHUB,
            new ProviderSubject("1849102"),
            "devpath-user",
            "DevPath User",
            null
        );

        AuthenticatedUser first = service.process(command);
        AuthenticatedUser second = service.process(command);

        assertThat(first.userId()).isEqualTo(second.userId());
        assertThat(users.values).hasSize(1);
        assertThat(identities.values).hasSize(1);
        assertThat(audit.events).contains(
            AuthenticationAuditEvent.EXTERNAL_IDENTITY_LINKED,
            AuthenticationAuditEvent.LOGIN_SUCCEEDED
        );
    }

    @Test
    void rejectsAnExistingDisabledInternalUser() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryExternalIdentityRepository identities = new InMemoryExternalIdentityRepository();
        User disabled = User.rehydrate(
            UserId.newId(),
            AccountStatus.SUSPENDED,
            "Disabled User",
            null,
            NOW,
            NOW,
            1
        );
        users.save(disabled);
        identities.save(ExternalIdentity.link(
            disabled.id(),
            OAuthProvider.GITHUB,
            new ProviderSubject("1849102"),
            "disabled-user",
            "Disabled User",
            null,
            NOW
        ));
        var service = new OAuthLoginApplicationService(
            users,
            identities,
            new RecordingAudit(),
            immediateTransactions(),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> service.process(new OAuthLoginCommand(
            OAuthProvider.GITHUB,
            new ProviderSubject("1849102"),
            "disabled-user",
            "Disabled User",
            null
        ))).isInstanceOf(DisabledAccountException.class);
    }

    @SuppressWarnings("unchecked")
    private TransactionOperations immediateTransactions() {
        TransactionOperations transactions = mock(TransactionOperations.class);
        when(transactions.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        return transactions;
    }

    private static final class InMemoryUserRepository implements UserRepositoryPort {
        private final Map<UserId, User> values = new HashMap<>();

        @Override
        public Optional<User> findById(UserId userId) {
            return Optional.ofNullable(values.get(userId));
        }

        @Override
        public User save(User user) {
            values.put(user.id(), user);
            return user;
        }
    }

    private static final class InMemoryExternalIdentityRepository implements ExternalIdentityRepositoryPort {
        private final Map<String, ExternalIdentity> values = new HashMap<>();

        @Override
        public Optional<ExternalIdentity> findByProviderAndSubject(
            OAuthProvider provider,
            ProviderSubject subject
        ) {
            return Optional.ofNullable(values.get(provider.name() + ":" + subject.value()));
        }

        @Override
        public ExternalIdentity save(ExternalIdentity identity) {
            values.put(identity.provider().name() + ":" + identity.providerSubject().value(), identity);
            return identity;
        }
    }

    private static final class RecordingAudit implements AuthenticationAuditPort {
        private final List<AuthenticationAuditEvent> events = new ArrayList<>();

        @Override
        public void record(AuthenticationAuditEvent event, java.util.UUID userId, OAuthProvider provider, Instant occurredAt) {
            events.add(event);
        }
    }
}
