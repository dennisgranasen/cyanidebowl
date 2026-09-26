package net.warp_scores.warpscores.ai.provider;
import java.time.Instant;
public class LlmProviderException extends RuntimeException {
    public enum Kind {
        AUTHENTICATION(false), RATE_LIMIT(true), TIMEOUT(true), UNAVAILABLE(true), REQUEST_TOO_LARGE(true), BAD_REQUEST(false), REJECTED(false), MALFORMED_RESPONSE(false), UNKNOWN(true);
        private final boolean retryable; Kind(boolean retryable){this.retryable=retryable;} public boolean retryable(){return retryable;}
    }
    public enum RateLimitScope { TEMPORARY, QUOTA_EXHAUSTED }

    private final String providerId; private final Kind kind; private final Integer statusCode; private final Instant retryAt; private final RateLimitScope rateLimitScope;
    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message, Throwable cause){this(providerId,kind,statusCode,message,cause,null,RateLimitScope.TEMPORARY);}
    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message, Throwable cause, Instant retryAt){this(providerId,kind,statusCode,message,cause,retryAt,RateLimitScope.TEMPORARY);}
    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message, Throwable cause, Instant retryAt, RateLimitScope rateLimitScope){super(message,cause);this.providerId=providerId;this.kind=kind;this.statusCode=statusCode;this.retryAt=retryAt;this.rateLimitScope=rateLimitScope == null ? RateLimitScope.TEMPORARY : rateLimitScope;}
    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message){this(providerId,kind,statusCode,message,null,null);}
    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message, Instant retryAt){this(providerId,kind,statusCode,message,null,retryAt);}
    public static boolean isQuotaExhaustedResponse(String body){if(body==null)return false;String value=body.toLowerCase(java.util.Locale.ROOT);return value.contains("insufficient_quota")||value.contains("billing_hard_limit")||value.contains("quota exhausted")||value.contains("quota exceeded")||value.contains("daily quota")||value.contains("daily limit")||value.contains("monthly quota")||value.contains("monthly limit")||value.contains("per day")||value.contains("per month");}
    public String providerId(){return providerId;} public Kind kind(){return kind;} public Integer statusCode(){return statusCode;} public Instant retryAt(){return retryAt;} public RateLimitScope rateLimitScope(){return rateLimitScope;} public boolean retryable(){return kind.retryable();}
}
