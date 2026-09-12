package net.warp_scores.warpscores.ai.provider;

/** Normalized provider failure consumed by fallback/retry policy outside provider adapters. */
public class LlmProviderException extends RuntimeException {
    public enum Kind {
        AUTHENTICATION(false),
        RATE_LIMIT(true),
        TIMEOUT(true),
        UNAVAILABLE(true),
        REQUEST_TOO_LARGE(true),
        BAD_REQUEST(false),
        REJECTED(false),
        MALFORMED_RESPONSE(false),
        UNKNOWN(true);

        private final boolean retryable;

        Kind(boolean retryable) {
            this.retryable = retryable;
        }

        public boolean retryable() {
            return retryable;
        }
    }

    private final String providerId;
    private final Kind kind;
    private final Integer statusCode;

    public LlmProviderException(
            String providerId,
            Kind kind,
            Integer statusCode,
            String message,
            Throwable cause) {
        super(message, cause);
        this.providerId = providerId;
        this.kind = kind;
        this.statusCode = statusCode;
    }

    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message) {
        this(providerId, kind, statusCode, message, null);
    }

    public String providerId() {
        return providerId;
    }

    public Kind kind() {
        return kind;
    }

    public Integer statusCode() {
        return statusCode;
    }

    public boolean retryable() {
        return kind.retryable();
    }
}
