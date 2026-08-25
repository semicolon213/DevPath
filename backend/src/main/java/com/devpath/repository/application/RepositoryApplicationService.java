package com.devpath.repository.application;

import com.devpath.identity.application.ExternalIdentityRepositoryPort;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.UserId;
import com.devpath.integration.application.GitHubIntegrationApplicationService;
import com.devpath.repository.domain.Repository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RepositoryApplicationService {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private final GitHubIntegrationApplicationService github;
    private final ExternalIdentityRepositoryPort identities;
    private final RepositoryRegistrationTransaction transactions;
    private final Clock clock;

    public RepositoryApplicationService(
        GitHubIntegrationApplicationService github,
        ExternalIdentityRepositoryPort identities,
        RepositoryRegistrationTransaction transactions,
        Clock clock
    ) {
        this.github = github;
        this.identities = identities;
        this.transactions = transactions;
        this.clock = clock;
    }

    public RepositoryView importGitHub(UUID userId, String providerRepositoryId) {
        String reference = validateProviderReference(providerRepositoryId);
        var providerRepository = github.listRepositories(userId).repositories().stream()
            .filter(repository -> repository.providerRepositoryId().equals(reference))
            .findFirst()
            .orElseThrow(RepositoryNotAccessibleException::new);
        var identity = identities.findByUserIdAndProvider(new UserId(userId), OAuthProvider.GITHUB)
            .orElseThrow(RepositoryNotAccessibleException::new);
        Instant now = clock.instant();
        Repository candidate = Repository.discover(
            userId, identity.id().value(), providerRepository.providerRepositoryId(), providerRepository.name(),
            providerRepository.fullName(), providerRepository.owner(), providerRepository.privateRepository(),
            providerRepository.defaultBranch(), providerRepository.archived(), providerRepository.htmlUrl(), now
        );
        return transactions.register(candidate, now);
    }

    public RepositoryListView list(UUID userId, Integer requestedLimit, String cursor, boolean includeArchived) {
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Repository page limit is invalid");
        }
        int offset = decodeCursor(cursor);
        if (offset % limit != 0) {
            throw new IllegalArgumentException("Repository cursor does not match the page limit");
        }
        long total = transactions.count(userId, includeArchived);
        var repositories = transactions.findPage(userId, offset / limit, limit, includeArchived).stream()
            .map(RepositoryView::from)
            .toList();
        int nextOffset = offset + repositories.size();
        String nextCursor = nextOffset < total ? encodeCursor(nextOffset) : null;
        return new RepositoryListView(repositories, limit, nextCursor, total);
    }

    public RepositoryView get(UUID userId, UUID repositoryId) {
        return transactions.get(userId, repositoryId);
    }

    public RepositoryView archive(UUID userId, UUID repositoryId) {
        return transactions.archive(userId, repositoryId, clock.instant());
    }

    public RepositoryView restore(UUID userId, UUID repositoryId) {
        return transactions.restore(userId, repositoryId, clock.instant());
    }

    private String validateProviderReference(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,63}")) {
            throw new IllegalArgumentException("GitHub repository reference is invalid");
        }
        return value;
    }

    private int decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int offset = Integer.parseInt(decoded);
            if (offset < 0) {
                throw new IllegalArgumentException("Repository cursor is invalid");
            }
            return offset;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Repository cursor is invalid", exception);
        }
    }

    private String encodeCursor(int offset) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(Integer.toString(offset).getBytes(StandardCharsets.UTF_8));
    }
}
