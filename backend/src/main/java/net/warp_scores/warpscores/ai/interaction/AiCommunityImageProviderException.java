package net.warp_scores.warpscores.ai.interaction;

public class AiCommunityImageProviderException extends RuntimeException {
    private final boolean retryable;

    public AiCommunityImageProviderException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
