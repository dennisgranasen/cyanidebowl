#!/usr/bin/env python3
from pathlib import Path
import subprocess

updates = {}

def read(path):
    p = Path(path)
    if not p.exists():
        raise RuntimeError(f"Missing expected file: {path}")
    return p.read_text(encoding="utf-8-sig")

def put(path, text):
    updates[path] = "\n".join(line.rstrip() for line in text.splitlines()) + "\n"

# ---------------------------------------------------------------------------
# AiProviderProperties: task-targets map
# ---------------------------------------------------------------------------
path = "backend/src/main/java/net/warp_scores/warpscores/ai/provider/AiProviderProperties.java"
text = read(path)

anchor = """    private List<ModelTargetConfig> defaultTargets = new ArrayList<>();
    private Map<String, List<ModelTargetConfig>> reporterTargets = new LinkedHashMap<>();
"""
replacement = """    private List<ModelTargetConfig> defaultTargets = new ArrayList<>();
    private Map<String, List<ModelTargetConfig>> reporterTargets = new LinkedHashMap<>();
    private Map<String, List<ModelTargetConfig>> taskTargets = new LinkedHashMap<>();
"""
if "private Map<String, List<ModelTargetConfig>> taskTargets" not in text:
    if anchor not in text:
        raise RuntimeError(f"{path}: routing property anchor not found")
    text = text.replace(anchor, replacement, 1)
put(path, text)

# ---------------------------------------------------------------------------
# LlmProviderRouter: preserve SAM compatibility, add default task-aware method.
# ---------------------------------------------------------------------------
path = "backend/src/main/java/net/warp_scores/warpscores/ai/provider/LlmProviderRouter.java"
text = read(path)

if "ContextTaskType" not in text:
    text = text.replace(
        "package net.warp_scores.warpscores.ai.provider;\n\n",
        "package net.warp_scores.warpscores.ai.provider;\n\n"
        "import net.warp_scores.warpscores.ai.context.ContextTaskType;\n",
        1,
    )

anchor = """public interface LlmProviderRouter {
    List<ModelTarget> targetsForReporter(String reporterId);

"""
replacement = """public interface LlmProviderRouter {
    List<ModelTarget> targetsForReporter(String reporterId);

    /**
     * Resolve ordered provider/model targets for a concrete task.
     *
     * <p>The default keeps existing router implementations and test lambdas
     * source-compatible. ConfiguredLlmProviderRouter overrides this to apply
     * task-specific routes.</p>
     */
    default List<ModelTarget> targetsForTask(
            String reporterId,
            ContextTaskType taskType) {
        return targetsForReporter(reporterId);
    }

"""
if "targetsForTask(" not in text:
    if anchor not in text:
        raise RuntimeError(f"{path}: interface anchor not found")
    text = text.replace(anchor, replacement, 1)
put(path, text)

# ---------------------------------------------------------------------------
# Configured router: task -> reporter -> defaults.
# ---------------------------------------------------------------------------
path = "backend/src/main/java/net/warp_scores/warpscores/ai/provider/ConfiguredLlmProviderRouter.java"
text = read(path)

if "ContextTaskType" not in text:
    text = text.replace(
        "import lombok.RequiredArgsConstructor;\n",
        "import lombok.RequiredArgsConstructor;\n"
        "import net.warp_scores.warpscores.ai.context.ContextTaskType;\n",
        1,
    )

old_method = """    @Override
    public List<ModelTarget> targetsForReporter(String reporterId) {
        List<AiProviderProperties.ModelTargetConfig> configured =
                properties.getReporterTargets().get(reporterId);
        if (configured == null || configured.isEmpty()) {
            configured = properties.getDefaultTargets();
        }
        return configured.stream().map(this::validatedTarget).toList();
    }

"""
new_method = """    @Override
    public List<ModelTarget> targetsForReporter(String reporterId) {
        return configuredReporterOrDefault(reporterId).stream()
                .map(this::validatedTarget)
                .toList();
    }

    @Override
    public List<ModelTarget> targetsForTask(
            String reporterId,
            ContextTaskType taskType) {
        List<AiProviderProperties.ModelTargetConfig> configured = null;

        if (taskType != null) {
            configured = properties.getTaskTargets().get(taskType.name());
        }
        if (configured == null || configured.isEmpty()) {
            configured = configuredReporterOrDefault(reporterId);
        }

        return configured.stream()
                .map(this::validatedTarget)
                .toList();
    }

    private List<AiProviderProperties.ModelTargetConfig> configuredReporterOrDefault(
            String reporterId) {
        List<AiProviderProperties.ModelTargetConfig> configured =
                properties.getReporterTargets().get(reporterId);
        if (configured == null || configured.isEmpty()) {
            configured = properties.getDefaultTargets();
        }
        return configured;
    }

"""
if "public List<ModelTarget> targetsForTask(" not in text:
    if old_method not in text:
        raise RuntimeError(f"{path}: current targetsForReporter method not recognized")
    text = text.replace(old_method, new_method, 1)
put(path, text)

# ---------------------------------------------------------------------------
# Execution: route using request.taskType()
# ---------------------------------------------------------------------------
path = "backend/src/main/java/net/warp_scores/warpscores/ai/provider/LlmExecutionService.java"
text = read(path)

old = """        List<LlmProviderRouter.ModelTarget> targets = router.targetsForReporter(reporterId);
"""
new = """        List<LlmProviderRouter.ModelTarget> targets =
                router.targetsForTask(reporterId, request.taskType());
"""
if old in text:
    text = text.replace(old, new, 1)
elif "router.targetsForTask(reporterId, request.taskType())" not in text:
    raise RuntimeError(f"{path}: router call not recognized")
put(path, text)

# ---------------------------------------------------------------------------
# Router test: task target must win over reporter/default.
# ---------------------------------------------------------------------------
path = "backend/src/test/java/net/warp_scores/warpscores/ai/provider/ConfiguredLlmProviderRouterTest.java"
text = read(path)

if "ContextTaskType" not in text:
    text = text.replace(
        "package net.warp_scores.warpscores.ai.provider;\n\n",
        "package net.warp_scores.warpscores.ai.provider;\n\n"
        "import net.warp_scores.warpscores.ai.context.ContextTaskType;\n",
        1,
    )

test_method = """    @Test
    void usesTaskTargetsBeforeReporterAndDefaultTargets() {
        AiProviderProperties props = new AiProviderProperties();
        props.setDefaultTargets(List.of(target("gemini", "default-model")));
        props.getReporterTargets().put(
                "putridia",
                List.of(target("openrouter", "reporter-model")));
        props.getTaskTargets().put(
                ContextTaskType.PLAYER_RATING.name(),
                List.of(target("cloudflare", "@cf/openai/gpt-oss-20b")));

        ConfiguredLlmProviderRouter router = new ConfiguredLlmProviderRouter(
                props,
                new LlmProviderRegistry(List.of(
                        provider("gemini"),
                        provider("openrouter"),
                        provider("cloudflare"))));

        assertThat(router.targetsForTask(
                "putridia",
                ContextTaskType.PLAYER_RATING))
                .containsExactly(new LlmProviderRouter.ModelTarget(
                        "cloudflare",
                        "@cf/openai/gpt-oss-20b"));

        assertThat(router.targetsForTask(
                "putridia",
                ContextTaskType.EDITORIAL_ARTICLE))
                .containsExactly(new LlmProviderRouter.ModelTarget(
                        "openrouter",
                        "reporter-model"));

        assertThat(router.targetsForTask(
                "other",
                ContextTaskType.EDITORIAL_ARTICLE))
                .containsExactly(new LlmProviderRouter.ModelTarget(
                        "gemini",
                        "default-model"));
    }

"""
if "usesTaskTargetsBeforeReporterAndDefaultTargets" not in text:
    marker = """    @Test
    void rejectsConfiguredUnknownProvider() {
"""
    if marker not in text:
        raise RuntimeError(f"{path}: test insertion anchor not found")
    text = text.replace(marker, test_method + marker, 1)
put(path, text)

# ---------------------------------------------------------------------------
# application.yml:
# - Cloudflare Workers AI as logical text provider through existing Responses adapter
# - task-specific routing for short/high-volume tasks
# ---------------------------------------------------------------------------
path = "backend/src/main/resources/application.yml"
text = read(path)

cf_endpoint = """        cloudflare:
          base-url: https://api.cloudflare.com/client/v4/accounts/${CLOUDFLARE_ACCOUNT_ID:}/ai/v1
          api-key: ${AI_API_KEY_CLOUDFLARE:}
          timeout: ${CLOUDFLARE_TEXT_TIMEOUT:60s}
          structured-output: true
          context-window-tokens: 128000
          max-output-tokens: 8192
"""
if "openai-compatible:\n        cloudflare:" not in text:
    anchor = """      openai-compatible:
"""
    if anchor not in text:
        raise RuntimeError(f"{path}: openai-compatible section not found")
    text = text.replace(anchor, anchor + cf_endpoint, 1)

task_routes = """    # Task-specific text routing. These routes take precedence over reporter-targets
    # and default-targets. Ordered entries are retry/fallback targets.
    task-targets:
      PLAYER_RATING:
        - provider-id: cloudflare
          model: ${AI_MODEL_PLAYER_RATING:@cf/openai/gpt-oss-20b}
        - provider-id: groq
          model: openai/gpt-oss-120b
        - provider-id: gemini
          model: gemini-3.6-flash
      ARTICLE_COMMENT:
        - provider-id: cloudflare
          model: ${AI_MODEL_ARTICLE_COMMENT:@cf/openai/gpt-oss-20b}
        - provider-id: groq
          model: openai/gpt-oss-120b
        - provider-id: gemini
          model: gemini-3.6-flash
      SOCIAL_REPLY:
        - provider-id: cloudflare
          model: ${AI_MODEL_SOCIAL_REPLY:@cf/openai/gpt-oss-20b}
        - provider-id: groq
          model: openai/gpt-oss-120b
        - provider-id: gemini
          model: gemini-3.6-flash

"""
if "    task-targets:\n" not in text:
    anchor = """    community-media:
"""
    if anchor not in text:
        raise RuntimeError(f"{path}: community-media anchor not found")
    text = text.replace(anchor, task_routes + anchor, 1)

put(path, text)

# ---------------------------------------------------------------------------
# Preflight succeeded; write all changes.
# ---------------------------------------------------------------------------
for p, content in updates.items():
    Path(p).write_text(content, encoding="utf-8")

subprocess.run(["git", "diff", "--check"], check=True)

print("Applied task-aware LLM routing.")
print()
print("Routing precedence:")
print("  task-targets -> reporter-targets -> default-targets")
print()
print("Default high-volume task routing:")
print("  PLAYER_RATING   -> cloudflare -> groq -> gemini")
print("  ARTICLE_COMMENT -> cloudflare -> groq -> gemini")
print("  SOCIAL_REPLY    -> cloudflare -> groq -> gemini")
print()
print("Cloudflare text provider:")
print("  base-url: .../accounts/${CLOUDFLARE_ACCOUNT_ID}/ai/v1")
print("  model: @cf/openai/gpt-oss-20b")
print("  token: AI_API_KEY_CLOUDFLARE")
print()
print("Existing EDITORIAL_ARTICLE / MATCH_REPORT / FAN_PROFILE behavior is unchanged")
print("unless you add those keys under warpscores.ai.task-targets.")
print()
print("Validate:")
print("  cd backend && ./mvnw clean test")
