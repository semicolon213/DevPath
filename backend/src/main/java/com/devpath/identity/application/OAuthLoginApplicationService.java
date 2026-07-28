package com.devpath.identity.application;

import com.devpath.identity.domain.DisabledAccountException;
import com.devpath.identity.domain.ExternalIdentity;
import com.devpath.identity.domain.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class OAuthLoginApplicationService implements ProcessOAuthLoginUseCase {
    private final UserRepositoryPort userRepository;
    private final ExternalIdentityRepositoryPort externalIdentityRepository;
    private final AuthenticationAuditPort auditPort;
    private final TransactionOperations transactions;
    private final Clock clock;

    public OAuthLoginApplicationService(
        UserRepositoryPort userRepository,
        ExternalIdentityRepositoryPort externalIdentityRepository,
        AuthenticationAuditPort auditPort,
        TransactionOperations transactions,
        Clock clock
    ) {
        this.userRepository = userRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.auditPort = auditPort;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public AuthenticatedUser process(OAuthLoginCommand command) {
        return externalIdentityRepository
            .findByProviderAndSubject(command.provider(), command.providerSubject())
            .map(identity -> resolveExisting(identity, command))
            .orElseGet(() -> provisionOrResolveConcurrent(command));
    }

    private AuthenticatedUser provisionOrResolveConcurrent(OAuthLoginCommand command) {
        try {
            AuthenticatedUser created = transactions.execute(status -> {
                var concurrentExisting = externalIdentityRepository
                    .findByProviderAndSubject(command.provider(), command.providerSubject());
                if (concurrentExisting.isPresent()) {
                    return resolveExisting(concurrentExisting.get(), command);
                }

                Instant now = clock.instant();
                User user = userRepository.save(User.register(command.displayName(), command.avatarUrl(), now));
                ExternalIdentity identity = ExternalIdentity.link(
                    user.id(),
                    command.provider(),
                    command.providerSubject(),
                    command.providerUsername(),
                    command.displayName(),
                    command.avatarUrl(),
                    now
                );
                externalIdentityRepository.save(identity);
                auditPort.record(AuthenticationAuditEvent.EXTERNAL_IDENTITY_LINKED, user.id().value(), command.provider(), now);
                auditPort.record(AuthenticationAuditEvent.LOGIN_SUCCEEDED, user.id().value(), command.provider(), now);
                return AuthenticatedUser.from(user, command.provider());
            });
            return Objects.requireNonNull(created, "Identity provisioning transaction returned no result");
        } catch (DuplicateExternalIdentityException exception) {
            return externalIdentityRepository
                .findByProviderAndSubject(command.provider(), command.providerSubject())
                .map(identity -> resolveExisting(identity, command))
                .orElseThrow(() -> exception);
        }
    }

    private AuthenticatedUser resolveExisting(ExternalIdentity identity, OAuthLoginCommand command) {
        User user = userRepository.findById(identity.userId()).orElseThrow(UserNotFoundException::new);
        try {
            user.assertAuthenticationAllowed();
        } catch (DisabledAccountException exception) {
            auditPort.record(
                AuthenticationAuditEvent.AUTHENTICATION_REJECTED_DISABLED_ACCOUNT,
                user.id().value(),
                command.provider(),
                clock.instant()
            );
            throw exception;
        }
        auditPort.record(AuthenticationAuditEvent.LOGIN_SUCCEEDED, user.id().value(), command.provider(), clock.instant());
        return AuthenticatedUser.from(user, command.provider());
    }
}
