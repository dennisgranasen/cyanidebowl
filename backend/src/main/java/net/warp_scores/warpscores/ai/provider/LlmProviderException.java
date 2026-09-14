package net.warp_scores.warpscores.ai.provider;
import java.time.Instant;
public class LlmProviderException extends RuntimeException {
    public enum Kind {
        AUTHENTICATION(false), RATE_LIMIT(true), TIMEOUT(true), UNAVAILABLE(true), REQUEST_TOO_LARGE(true), BAD_REQUEST(false), REJECTED(false), MALFORMED_RESPONSE(false), UNKNOWN(true);
        private final boolean retryable; Kind(boolean retryable){this.retryable=retryable;} public boolean retryable(){return retryable;}
    }
    private final String providerId; private final Kind kind; private final Integer statusCode; private final Instant retryAt;
    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message, Throwable cause){this(providerId,kind,statusCode,message,cause,null);}
    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message, Throwable cause, Instant retryAt){super(message,cause);this.providerId=providerId;this.kind=kind;this.statusCode=statusCode;this.retryAt=retryAt;}
    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message){this(providerId,kind,statusCode,message,null,null);}
    public LlmProviderException(String providerId, Kind kind, Integer statusCode, String message, Instant retryAt){this(providerId,kind,statusCode,message,null,retryAt);}
    public String providerId(){return providerId;} public Kind kind(){return kind;} public Integer statusCode(){return statusCode;} public Instant retryAt(){return retryAt;} public boolean retryable(){return kind.retryable();}
}
