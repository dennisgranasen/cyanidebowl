#!/usr/bin/env python3
from pathlib import Path
import re
import subprocess

candidates = [
    Path("backend/src/main/java/net/warp_scores/warpscores/ai/provider/gemini/GeminiInteractionsLlmProvider.java"),
    Path("backend/src/main/java/net/warp_scores/warpscores/ai/provider/gemini/GeminiInteractionLlmProvider.java"),
    Path("backend/src/main/java/net/warp_scores/warpscores/ai/provider/gemini/GeminiLlmProvider.java"),
]
path = next((p for p in candidates if p.exists()), None)
if path is None:
    raise RuntimeError(
        "Could not find Gemini provider file under backend/.../ai/provider/gemini"
    )

text = path.read_text(encoding="utf-8-sig")
original = text

if "throw httpFailure(response.statusCode());" in text:
    text = text.replace(
        "throw httpFailure(response.statusCode());",
        'throw httpFailure(\n'
        '                        response.statusCode(),\n'
        '                        response.body(),\n'
        '                        response.headers().firstValue("Retry-After").orElse(null));',
        1,
    )
elif "httpFailure(response.statusCode(), response.body()" not in text:
    raise RuntimeError(
        f"{path}: Gemini HTTP failure call shape not recognized; refusing to guess"
    )

old_method = (
    "    private static LlmProviderException httpFailure(int status) {\n"
    "        LlmProviderException.Kind kind = switch (status) {\n"
    "            case 401, 403 -> LlmProviderException.Kind.AUTHENTICATION;\n"
    "            case 408, 504 -> LlmProviderException.Kind.TIMEOUT;\n"
    "            case 429 -> LlmProviderException.Kind.RATE_LIMIT;\n"
    "            default -> status >= 500\n"
    "                    ? LlmProviderException.Kind.UNAVAILABLE\n"
    "                    : LlmProviderException.Kind.BAD_REQUEST;\n"
    "        };\n"
    '        return new LlmProviderException(ID, kind, status, "Gemini HTTP " + status);\n'
    "    }\n"
)

new_method = (
    "    private static LlmProviderException httpFailure(\n"
    "            int status,\n"
    "            String body,\n"
    "            String retryAfter) {\n"
    "        LlmProviderException.Kind kind = switch (status) {\n"
    "            case 401, 403 -> LlmProviderException.Kind.AUTHENTICATION;\n"
    "            case 408, 504 -> LlmProviderException.Kind.TIMEOUT;\n"
    "            case 429 -> LlmProviderException.Kind.RATE_LIMIT;\n"
    "            default -> status >= 500\n"
    "                    ? LlmProviderException.Kind.UNAVAILABLE\n"
    "                    : LlmProviderException.Kind.BAD_REQUEST;\n"
    "        };\n\n"
    "        String detail = sanitizeBody(body, 700);\n"
    '        StringBuilder message = new StringBuilder("Gemini HTTP ").append(status);\n'
    "        if (retryAfter != null && !retryAfter.isBlank()) {\n"
    '            message.append(" (Retry-After: ").append(retryAfter).append(")");\n'
    "        }\n"
    "        if (!detail.isBlank()) {\n"
    '            message.append(": ").append(detail);\n'
    "        }\n\n"
    "        return new LlmProviderException(\n"
    "                ID,\n"
    "                kind,\n"
    "                status,\n"
    "                message.toString());\n"
    "    }\n\n"
    "    private static String sanitizeBody(String body, int maxLength) {\n"
    '        if (body == null || body.isBlank()) return "";\n\n'
    "        String value = body\n"
    '                .replaceAll("\\\\s+", " ")\n'
    "                .trim();\n\n"
    "        if (value.length() <= maxLength) return value;\n"
    '        return value.substring(0, maxLength) + "…";\n'
    "    }\n"
)

if old_method in text:
    text = text.replace(old_method, new_method, 1)
elif "private static String sanitizeBody(" not in text:
    pattern = re.compile(
        r"    private static LlmProviderException httpFailure\(int status\) \{.*?\n    \}\n",
        re.DOTALL,
    )
    text, count = pattern.subn(new_method, text, count=1)
    if count != 1:
        raise RuntimeError(
            f"{path}: Gemini httpFailure(int status) method not recognized"
        )

if text == original:
    print(f"No changes needed in {path}")
else:
    path.write_text(text, encoding="utf-8")
    print(f"Updated {path}")

subprocess.run(["git", "diff", "--check"], check=True)

print()
print("Gemini provider errors now include:")
print("- HTTP status")
print("- Retry-After header, when present")
print("- whitespace-normalized response body, truncated to 700 chars")
print()
print("Validate:")
print("  cd backend && ./mvnw test")
