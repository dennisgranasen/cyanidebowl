package net.warp_scores.warpscores.ai.interaction;
import java.time.Instant;
public class AiCommunityImageProviderException extends RuntimeException {
    private final boolean retryable; private final Integer statusCode; private final Instant retryAt;
    public AiCommunityImageProviderException(String message, boolean retryable){this(message,retryable,null,null);}
    public AiCommunityImageProviderException(String message, boolean retryable, Integer statusCode, Instant retryAt){super(message);this.retryable=retryable;this.statusCode=statusCode;this.retryAt=retryAt;}
    public boolean isRetryable(){return retryable;} public Integer statusCode(){return statusCode;} public Instant retryAt(){return retryAt;}
}
