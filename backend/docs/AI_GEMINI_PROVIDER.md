# Gemini provider adapter

AI-006 implements the `gemini` `LlmProvider` using Google's current Gemini Interactions API.

## API boundary

The adapter calls:

```text
POST https://generativelanguage.googleapis.com/v1beta/interactions
```

It is deliberately stateless (`store=false`, `stream=false`). Canonical context retrieval,
ranking and world-model policy happen before this adapter.

Structured JSON output maps to Interactions `response_format`:

```json
{
  "type": "text",
  "mime_type": "application/json",
  "schema": { "...": "canonical output schema" }
}
```

The response is normalized into `CanonicalLlmResponse` using:
- interaction `id` as `providerRequestId`;
- returned `model`;
- model-output text from `steps`;
- `usage.total_input_tokens`;
- `usage.total_output_tokens`;
- interaction `status` as finish reason.

## Configuration

```yaml
warpscores:
  ai:
    providers:
      gemini:
        api-key: ${AI_API_KEY_GEMINI:}
        base-url: https://generativelanguage.googleapis.com/v1beta
        timeout: 60s
```

Keep `AI_API_KEY_GEMINI` outside source control.

Provider errors are normalized to `LlmProviderException`; retry/fallback policy belongs to
the orchestration layer, not this adapter.
