package net.warp_scores.warpscores.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prevents duplicate concurrent LLM generations for the same match/reporter pair.
 *
 * <p>This is deliberately process-local. If the backend is horizontally scaled,
 * this seam can be replaced by a distributed lease without changing callers.</p>
 */
@Component
public class MatchArticleGenerationGuard {
    private final Set<Key> inFlight = ConcurrentHashMap.newKeySet();

    public Lease acquire(String matchId, String reporterId) {
        Key key = new Key(matchId, reporterId);
        if (!inFlight.add(key)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An article generation for this match and reporter is already in progress");
        }
        return new Lease(key);
    }

    public final class Lease implements AutoCloseable {
        private final Key key;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private Lease(Key key) {
            this.key = key;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                inFlight.remove(key);
            }
        }
    }

    private record Key(String matchId, String reporterId) {}
}
