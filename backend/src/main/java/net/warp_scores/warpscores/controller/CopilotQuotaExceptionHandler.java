package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.ai.provider.copilot.CopilotQuotaGuard;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CopilotQuotaExceptionHandler {
    @ExceptionHandler(CopilotQuotaGuard.QuotaConfirmationRequiredException.class)
    public ResponseEntity<ConfirmationRequired> confirmationRequired(
            CopilotQuotaGuard.QuotaConfirmationRequiredException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConfirmationRequired("COPILOT_QUOTA_CONFIRMATION_REQUIRED", failure.getMessage()));
    }

    public record ConfirmationRequired(String code, String message) {
    }
}
