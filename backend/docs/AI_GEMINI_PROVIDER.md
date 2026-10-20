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

## Community image generation

Community-media jobs can also be routed to Gemini's Nano Banana image model through
the Interactions API. The image renderer uses `AI_API_KEY_GEMINI` and can be selected
as the `gemini` image provider in the community-media queue. Image input references
are sent as inline image content when present.

```yaml
warpscores:
  ai:
    providers:
      gemini:
        api-key: ${AI_API_KEY_GEMINI:}
        base-url: https://generativelanguage.googleapis.com/v1beta
        image-model: ${GEMINI_IMAGE_MODEL:gemini-nano-banana-2.1}
        image-timeout: ${GEMINI_IMAGE_TIMEOUT:3m}
```

The image model defaults to `gemini-nano-banana-2.1`; override it with
`GEMINI_IMAGE_MODEL` when testing another supported Gemini image model.

When `AI_COPILOT_ENABLED=true`, community-media jobs also expose
`Copilot SDK + Gemini Nano Banana`. That option runs the saved prompt through a
Copilot SDK session with a single image-generation tool; the tool calls the
configured Gemini Nano Banana image model and returns the resulting image block.
The Copilot model defaults to `gpt-6-luna` and can be overridden with
`AI_COPILOT_IMAGE_MODEL`. This is an SDK-orchestrated Gemini image request, not
a native Copilot image model.
