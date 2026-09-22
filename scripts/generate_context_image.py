#!/usr/bin/env python3
from __future__ import annotations

"""
Generate or replace a Star Player runtime context image from a manually chosen
image file.

Examples:
    python3 scripts/generate_context_image.py \
      data/starplayers/candidates/Griff_Oberwald/01_fandom_Griff_Oberwald.jpg

    python3 scripts/generate_context_image.py \
      data/starplayers/candidates/Griff_Oberwald/01_fandom_Griff_Oberwald.jpg \
      --force

    python3 scripts/generate_context_image.py ~/tmp/griff-crop.png --player Griff_Oberwald --force
"""

import argparse
import io
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow is required. Install it with: python3 -m pip install Pillow")

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data/starplayers"
MAX_DIMENSION = 511


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def safe_id(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9._-]+", "_", value).strip("_")


def load_manifest() -> dict:
    manifest_path = DATA / "manifest.json"
    if not manifest_path.exists():
        sys.exit(f"Missing manifest: {manifest_path}. Run sync script first.")
    return json.loads(manifest_path.read_text(encoding="utf-8"))


def find_player_stem(manifest: dict, input_path: Path, explicit: str | None) -> tuple[str, str]:
    players = manifest.get("players", {})
    stem_to_name = {safe_id(player_id): meta.get("name", player_id) for player_id, meta in players.items()}

    if explicit:
        exp = safe_id(explicit)
        if exp in stem_to_name:
            return exp, stem_to_name[exp]
        for stem, name in stem_to_name.items():
            if name.lower() == explicit.lower():
                return stem, name
        sys.exit(f"Could not resolve --player {explicit!r}")

    candidates = []
    for part in [input_path.stem, *input_path.parts]:
        token = safe_id(str(part))
        if token in stem_to_name and token not in candidates:
            candidates.append(token)

    if len(candidates) == 1:
        stem = candidates[0]
        return stem, stem_to_name[stem]

    for stem in stem_to_name:
        if safe_id(input_path.stem).startswith(stem):
            return stem, stem_to_name[stem]

    sys.exit(
        "Could not infer which player this image belongs to. "
        "Pass --player <safe_id_or_name>."
    )


def normalize_image(data: bytes) -> tuple[bytes, int, int]:
    with Image.open(io.BytesIO(data)) as image:
        image.load()
        image = image.convert("RGBA")
        image.thumbnail((MAX_DIMENSION, MAX_DIMENSION), Image.Resampling.LANCZOS)
        width, height = image.size
        output = io.BytesIO()
        image.save(output, "PNG", optimize=True)
        return output.getvalue(), width, height


def next_version_path(base: Path) -> Path:
    if not base.exists():
        return base
    n = 2
    while True:
        candidate = base.with_name(f"{base.stem}.v{n}{base.suffix}")
        if not candidate.exists():
            return candidate
        n += 1


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("path", type=Path, help="Input image path")
    parser.add_argument("--player", help="Safe player id or exact player name")
    parser.add_argument("--force", action="store_true", help="Replace the active reference image")
    args = parser.parse_args()

    if not args.path.exists():
        sys.exit(f"Input file does not exist: {args.path}")

    manifest = load_manifest()
    stem, player_name = find_player_stem(manifest, args.path, args.player)

    references = DATA / "references"
    metadata = DATA / "reference-meta"
    references.mkdir(parents=True, exist_ok=True)
    metadata.mkdir(parents=True, exist_ok=True)

    encoded, width, height = normalize_image(args.path.read_bytes())

    active = references / f"{stem}.png"
    if args.force:
        target = active
        if target.exists():
            target.unlink()
    else:
        target = next_version_path(active)

    target.write_bytes(encoded)
    meta_path = metadata / f"{target.stem}.json"
    meta_path.write_text(
        json.dumps({
            "player": player_name,
            "sourceKind": "manual",
            "sourcePath": str(args.path.resolve()),
            "generatedAt": now_iso(),
            "width": width,
            "height": height,
            "replacesActive": bool(args.force),
        }, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"Player: {player_name}")
    print(f"Wrote: {target}")
    print(f"Meta:  {meta_path}")
    if not args.force and target != active:
        print("Review variant created. Rename it to the active filename or rerun with --force if you want it to replace the active context image.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
