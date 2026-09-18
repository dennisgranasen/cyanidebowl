#!/usr/bin/env python3
"""
Synchronize Blood Bowl star-player editorial assets from the versioned BBBase
snapshot referenced by backend/src/main/resources/ai/starplayers.json.

Generated assets are runtime/cache data and should NOT be committed.
Requires Pillow:
    python3 -m pip install Pillow

This version:
- trims excess background automatically
- optionally removes miniature base plates with a conservative heuristic
- rescales the result to a max dimension of 511 px

Usage:
    python3 scripts/sync_starplayer_assets.py
    python3 scripts/sync_starplayer_assets.py --output-dir /data/starplayers
    python3 scripts/sync_starplayer_assets.py --force
    python3 scripts/sync_starplayer_assets.py --keep-bases
"""
from __future__ import annotations

import argparse
import io
import json
import re
import sys
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow is required. Install it with: python3 -m pip install Pillow")

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "backend/src/main/resources/ai/starplayers.json"
DEFAULT_OUTPUT = ROOT / "data/starplayers"
MAX_DIMENSION = 511
USER_AGENT = "cyanidebowl-starplayer-sync/2.0"


def safe_id(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9._-]+", "_", value).strip("_")


def raw_github_url(url: str) -> str:
    match = re.match(r"https://github\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.*)", url)
    if match:
        owner, repo, ref, path = match.groups()
        return f"https://raw.githubusercontent.com/{owner}/{repo}/{ref}/{path}"
    return url


def download(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


def estimate_background(image: Image.Image, sample_size: int = 16) -> tuple[int, int, int]:
    width, height = image.size
    pixels = image.load()
    samples = []
    regions = [
        (0, 0, min(sample_size, width), min(sample_size, height)),
        (max(0, width - sample_size), 0, width, min(sample_size, height)),
        (0, max(0, height - sample_size), min(sample_size, width), height),
        (max(0, width - sample_size), max(0, height - sample_size), width, height),
    ]
    for x0, y0, x1, y1 in regions:
        for y in range(y0, y1):
            for x in range(x0, x1):
                r, g, b, a = pixels[x, y]
                if a > 32:
                    samples.append((r, g, b))
    if not samples:
        return (255, 255, 255)
    count = len(samples)
    return (
        sum(r for r, _, _ in samples) // count,
        sum(g for _, g, _ in samples) // count,
        sum(b for _, _, b in samples) // count,
    )


def is_foreground(pixel: tuple[int, int, int, int], background: tuple[int, int, int]) -> bool:
    r, g, b, a = pixel
    if a <= 24:
        return False
    distance = abs(r - background[0]) + abs(g - background[1]) + abs(b - background[2])
    return distance > 36 or a < 250


def trim_to_subject(image: Image.Image, margin_ratio: float = 0.04) -> Image.Image:
    width, height = image.size
    bg = estimate_background(image)
    pixels = image.load()
    left, top = width, height
    right, bottom = -1, -1

    for y in range(height):
        for x in range(width):
            if is_foreground(pixels[x, y], bg):
                if x < left:
                    left = x
                if y < top:
                    top = y
                if x > right:
                    right = x
                if y > bottom:
                    bottom = y

    if right < left or bottom < top:
        return image

    pad = max(2, int(round(max(width, height) * margin_ratio)))
    left = max(0, left - pad)
    top = max(0, top - pad)
    right = min(width - 1, right + pad)
    bottom = min(height - 1, bottom + pad)
    return image.crop((left, top, right + 1, bottom + 1))


def detect_base_cut(image: Image.Image) -> int | None:
    """
    Conservative heuristic:
    look for a dark, wide band low in the image that is likely a miniature base.
    Returns the y coordinate (exclusive) to crop to, or None if no clear base exists.
    """
    width, height = image.size
    bg = estimate_background(image)
    pixels = image.load()

    coverage = []
    dark_fraction = []
    mean_luma = []

    for y in range(height):
        fg = 0
        dark = 0
        luma_sum = 0.0
        for x in range(width):
            px = pixels[x, y]
            if is_foreground(px, bg):
                fg += 1
                r, g, b, _ = px
                luma = 0.2126 * r + 0.7152 * g + 0.0722 * b
                luma_sum += luma
                if luma < 95:
                    dark += 1
        cov = fg / width if width else 0.0
        coverage.append(cov)
        dark_fraction.append((dark / fg) if fg else 0.0)
        mean_luma.append((luma_sum / fg) if fg else 255.0)

    start = int(height * 0.68)
    end = int(height * 0.95)

    for y in range(start, max(start, end - 2)):
        cov_now = sum(coverage[y:y + 3]) / max(1, len(coverage[y:y + 3]))
        dark_now = sum(dark_fraction[y:y + 3]) / max(1, len(dark_fraction[y:y + 3]))
        luma_now = sum(mean_luma[y:y + 3]) / max(1, len(mean_luma[y:y + 3]))

        prev_from = max(0, y - 8)
        cov_prev = sum(coverage[prev_from:y]) / max(1, y - prev_from)

        # Wide + dark + sudden widening near the bottom => likely base
        if cov_now >= 0.42 and dark_now >= 0.28 and luma_now <= 125 and cov_now >= cov_prev + 0.07:
            cut = max(int(height * 0.60), y - max(2, int(round(height * 0.01))))
            # Safety: do not remove almost the whole lower body
            if cut >= int(height * 0.70):
                return cut

    return None


def remove_base_if_present(image: Image.Image) -> Image.Image:
    cut = detect_base_cut(image)
    if cut is None:
        return image
    return image.crop((0, 0, image.width, cut))


def normalize_image(data: bytes, keep_bases: bool = False) -> tuple[bytes, int, int]:
    with Image.open(io.BytesIO(data)) as image:
        image.load()
        image = image.convert("RGBA")
        image = trim_to_subject(image)
        if not keep_bases:
            image = remove_base_if_present(image)
            image = trim_to_subject(image, margin_ratio=0.03)
        image.thumbnail((MAX_DIMENSION, MAX_DIMENSION), Image.Resampling.LANCZOS)
        width, height = image.size
        output = io.BytesIO()
        image.save(output, "PNG", optimize=True)
        return output.getvalue(), width, height


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, default=CATALOG)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--keep-bases", action="store_true", help="Keep miniature bases in the normalized reference images")
    args = parser.parse_args()

    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    output = args.output_dir.resolve()
    references = output / "references"
    profiles = output / "profiles"
    references.mkdir(parents=True, exist_ok=True)
    profiles.mkdir(parents=True, exist_ok=True)

    manifest = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "sourceCommit": catalog.get("sourceCommit"),
        "maxDimension": MAX_DIMENSION,
        "keepBases": args.keep_bases,
        "players": {},
    }

    failures = []
    players = catalog.get("players", [])
    for index, player in enumerate(players, 1):
        player_id = player["id"]
        stem = safe_id(player_id)
        image_path = references / f"{stem}.png"
        profile_path = profiles / f"{stem}.md"

        try:
            if args.force or not profile_path.exists():
                profile_url = raw_github_url(player["sourceUrl"])
                profile_path.write_bytes(download(profile_url))

            if args.force or not image_path.exists():
                encoded, width, height = normalize_image(download(player["imageUrl"]), keep_bases=args.keep_bases)
                image_path.write_bytes(encoded)
            else:
                with Image.open(image_path) as existing:
                    width, height = existing.size

            manifest["players"][player_id] = {
                "name": player["name"],
                "reference": f"references/{image_path.name}",
                "profile": f"profiles/{profile_path.name}",
                "width": width,
                "height": height,
                "sourceUrl": player["sourceUrl"],
                "imageSourceUrl": player["imageUrl"],
            }
            print(f"[{index:3}/{len(players):3}] {player['name']}: {width}x{height}")
        except Exception as exc:
            failures.append((player_id, str(exc)))
            print(f"FAILED {player['name']}: {exc}", file=sys.stderr)

    (output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"\nAssets: {output}")
    print(f"Synced: {len(manifest['players'])}")
    if failures:
        print(f"Failed: {len(failures)}", file=sys.stderr)
        for player_id, error in failures:
            print(f"  {player_id}: {error}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
