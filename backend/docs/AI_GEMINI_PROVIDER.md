# Gemini provider

The text and community-image adapters use Google's Interactions API. Application
context is assembled before provider calls; adapters do not retrieve or reinterpret
domain context.

Community media can select Gemini directly. The Copilot SDK option is an orchestration
route to the same Gemini image renderer, not a separate image-generation model. Image
references are passed as inline image content.

Configuration is defined in `.env.example` and `application.yml`. Text-provider errors
are normalized by the adapter; retry and fallback policy belongs to the orchestration
layer.
