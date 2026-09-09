# Suggested configuration

```yaml
warpscores:
  ai-reporting:
    enabled: false
    second-report-probability: 0.10
    interaction-delay-seconds: 30
    max-initial-ai-comments: 2
    maximum-thread-depth: 4
    prompt-version: v1
    narrative-facts-version: v1
```

Credentials should come from environment/config:

```text
GROQ_API_KEY=...
OPENROUTER_API_KEY=...
```

Conceptual provider config:

```yaml
warpscores:
  llm:
    providers:
      groq:
        base-url: https://api.groq.com/openai/v1
        api-key: ${GROQ_API_KEY:}
      openrouter:
        base-url: https://openrouter.ai/api/v1
        api-key: ${OPENROUTER_API_KEY:}
```

Do not store credentials in reporter Markdown files or Mongo reporter profiles.
