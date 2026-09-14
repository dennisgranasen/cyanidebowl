#!/usr/bin/env python3
from pathlib import Path

path = Path("frontend/src/components/contest/ReplayAnalysisPanel.jsx")
text = path.read_text(encoding="utf-8")

replacements = [
(
"""const formatSuccessTotal = (row) => row
  ? `${displayZeroBlank(row.success)}/${displayZeroBlank(row.total)}`
  : '—';
""",
"""const formatSuccessTotal = (row) => row
  ? `${Number(row.success || 0)}/${Number(row.total || 0)}`
  : '—';
""",
"success/total ratio"
),
(
"""  const display = (row) => row
    ? `${displayZeroBlank(row.success)} / ${displayZeroBlank(row.neutral)} / ${displayZeroBlank(row.fail)} · ${displayZeroBlank(row.total)}`
    : '—';
""",
"""  const display = (row) => row
    ? `${Number(row.success || 0)} / ${Number(row.neutral || 0)} / ${Number(row.fail || 0)} · ${Number(row.total || 0)}`
    : '—';
""",
"special action compound result"
),
]

for old, new, label in replacements:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(
            f"{path}: {label}: expected exactly one match, found {count}. No files were written."
        )
    text = text.replace(old, new, 1)

path.write_text(text, encoding="utf-8")
print(f"Updated {path}")
print("Next:")
print("  git diff --check")
print("  git diff -- frontend/src/components/contest/ReplayAnalysisPanel.jsx")
