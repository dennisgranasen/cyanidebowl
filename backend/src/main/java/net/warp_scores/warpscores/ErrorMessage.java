package net.warp_scores.warpscores;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ErrorMessage {

    private final String message;

    public static ErrorMessage from(final String message) {
        return new ErrorMessage(message);
    }
}
