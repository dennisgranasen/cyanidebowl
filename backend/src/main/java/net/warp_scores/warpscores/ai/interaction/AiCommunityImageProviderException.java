package net.warp_scores.warpscores.ai.interaction;
import java.time.Instant;
public class AiCommunityImageProviderException extends RuntimeException {
    private final boolean retryable; private final Integer statusCode; private final Instant retryAt; private final boolean quotaExhausted;
    public AiCommunityImageProviderException(String message, boolean retryable){this(message,retryable,null,null,false);}
    public AiCommunityImageProviderException(String message, boolean retryable, Integer statusCode, Instant retryAt){this(message,retryable,statusCode,retryAt,false);}
    public AiCommunityImageProviderException(String message, boolean retryable, Integer statusCode, Instant retryAt, boolean quotaExhausted){
        super(message);this.retryable=retryable;this.statusCode=statusCode;this.retryAt=retryAt;this.quotaExhausted=quotaExhausted;
    }
    public boolean isRetryable(){return retryable;} public Integer statusCode(){return statusCode;} public Instant retryAt(){return retryAt;} public boolean quotaExhausted(){return quotaExhausted;}
}
