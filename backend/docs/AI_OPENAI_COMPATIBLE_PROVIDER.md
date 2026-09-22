# OpenAI-compatible provider adapter

AI-007 adds one reusable adapter for services implementing the OpenAI Responses API.

The adapter is intentionally named by protocol, not vendor. Each configured endpoint becomes
a logical `LlmProvider` with its own provider id, base URL and credential.

Example:

```yaml
warpscores:
  ai:
    providers:
      openai-compatible:
        grok:
          base-url: https://api.x.ai/v1
          api-key: ${AI_API_KEY_GROK:}
          timeout: 60s
          structured-output: true

    default-targets:
      - provider-id: gemini
        model: gemini-...
      - provider-id: grok
        model: grok-4.6
```

Additional compatible services can reuse the same adapter:

```yaml
warpscores:
  ai:
    providers:
      openai-compatible:
        local-llm:
          base-url: http://localhost:8000/v1
          api-key: ${AI_API_KEY_LOCAL:}
          structured-output: false
```

Routing therefore selects a logical provider instance plus model. Agent identity remains
independent from transport/provider configuration.

The implementation targets `/responses`, not legacy `/chat/completions`.

`LlmExecutionService` executes the ordered targets from `LlmProviderRouter` and only falls
back for normalized retryable provider failures. Authentication and bad-request failures
fail fast instead of silently trying another credential/service.
