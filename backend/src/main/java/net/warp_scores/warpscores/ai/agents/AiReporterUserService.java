package net.warp_scores.warpscores.ai.agents;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReporterUserService {
    private final AiReporterRegistry registry;
    private final WarpScoresUserRepository users;
    private final SequenceGenerator sequenceGenerator;

    @PostConstruct
    void reconcile() {
        for (AiReporterDefinition reporter : registry.all()) {
            String subject = reporter.resolvedUserSubject();
            WarpScoresUser user = users.findByAuthSubject(subject)
                    .map(existing -> reconcileExisting(existing, reporter))
                    .orElseGet(() -> createUser(subject, reporter));
            reporter.setUserId(user.getId());
        }
    }

    private WarpScoresUser createUser(String subject, AiReporterDefinition reporter) {
        WarpScoresUser user = new WarpScoresUser();
        user.setId(sequenceGenerator.nextIdFor(WarpScoresUser.class));
        user.setUsername(reporter.getAlias());
        user.setProvider("ai");
        user.setAuthSubject(subject);
        user.setAccountType(AccountType.AI);
        WarpScoresUser saved = users.save(user);
        log.info("Created AI-backed user {} for reporter {}", saved.getId(), reporter.getId());
        return saved;
    }

    private WarpScoresUser reconcileExisting(WarpScoresUser user, AiReporterDefinition reporter) {
        AccountType accountType = user.getAccountType();
        boolean legacyAiUser = accountType == null && "ai".equalsIgnoreCase(user.getProvider());
        if (accountType != AccountType.AI && !legacyAiUser) {
            throw new IllegalStateException(
                    "AI reporter '" + reporter.getId() + "' resolves to non-AI user " + user.getId());
        }

        boolean changed = false;
        if (accountType != AccountType.AI) {
            user.setAccountType(AccountType.AI);
            changed = true;
        }
        if (!reporter.getAlias().equals(user.getUsername())) {
            user.setUsername(reporter.getAlias());
            changed = true;
        }
        if (!"ai".equalsIgnoreCase(user.getProvider())) {
            user.setProvider("ai");
            changed = true;
        }
        return changed ? users.save(user) : user;
    }
}
