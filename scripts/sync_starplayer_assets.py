#!/usr/bin/env python3
from __future__ import annotations

"""
Review-friendly Star Player asset sync.

1. Uses Cyanidebowl's committed starplayers.json as the canonical current
   Star Player set.
2. Uses Fandom All_Players as the discovery/index source for historical player
   pages and context.
3. Collects free automatic image candidates per Star Player:
   - Fandom page image
   - BBBase image fallback
   4. Scores the available candidates and downloads them for manual review.
5. Saves untouched originals under data/starplayers/candidates/<player>/...
6. Generates the active runtime context image under
   data/starplayers/references/<safeid>.png

Manual review workflow:
- Review downloaded originals in data/starplayers/candidates/<player>/
- You may also save any image you find manually anywhere on disk.
- To turn a candidate or manually downloaded image into a runtime context image:
      python3 scripts/generate_context_image.py <path> --player <player>
  The --player option is optional when the player can be inferred from the path.
- Add --force to replace the active reference immediately.
- Without --force a versioned review image is created beside the active one.

Notes:
- Fandom/web/manual images are preserved compositionally and only scaled down.
- BBBase miniature photos use the conservative crop/base-removal pipeline.
- If --force is used, per-player candidate folders are cleared before refresh.

Optional Google image search:
The script uses Google Custom Search JSON API if these env vars exist:
    GOOGLE_CSE_API_KEY
    GOOGLE_CSE_CX
Without them, only Fandom + BBBase candidates are used.
"""

import argparse
import difflib
import io
import json
import re
import shutil
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow is required. Install it with: python3 -m pip install Pillow")

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "backend/src/main/resources/ai/starplayers.json"
DEFAULT_OUTPUT = ROOT / "data/starplayers"

FANDOM_API = "https://blood-bowl.fandom.com/api.php"
FANDOM_BASE = "https://blood-bowl.fandom.com/wiki/"
FANDOM_INDEX_PAGE = "All_Players"

MAX_DIMENSION = 511
USER_AGENT = "cyanidebowl-starplayer-sync/5.2"
BATCH_SIZE = 20
NEAR_EQUAL_MARGIN = 8.0
MAX_DOWNLOADED_CANDIDATES = 5

ALIASES = {
    "ivan deathshroud": [
        "Ivan 'The Animal' Deathshroud",
        'Ivan "The Animal" Deathshroud',
    ],
    "morg n thorg": ["Morg 'N' Thorg", "Morg 'n' Thorg"],
    "count luthor von drakenborg": ["Count Luthor von Drakenborg"],
}


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def api(params: dict[str, Any]) -> dict[str, Any]:
    query = urllib.parse.urlencode({
        **params,
        "format": "json",
        "formatversion": 2,
        "origin": "*",
    }, doseq=True)
    request = urllib.request.Request(
        f"{FANDOM_API}?{query}",
        headers={"User-Agent": USER_AGENT, "Accept": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=90) as response:
        return json.loads(response.read().decode("utf-8"))


def download(url: str, referer: str | None = None) -> bytes:
    headers = {
        "User-Agent": (
            "Mozilla/5.0 (X11; Linux x86_64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/153.0 Safari/537.36"
        ),
        "Accept": "image/avif,image/webp,image/apng,image/png,image/jpeg,*/*;q=0.8",
    }
    if referer:
        headers["Referer"] = referer

    request = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=90) as response:
            return response.read()
    except urllib.error.HTTPError as exc:
        if exc.code != 403 or referer:
            raise
        retry = urllib.request.Request(
            url,
            headers={**headers, "Referer": "https://blood-bowl.fandom.com/"},
        )
        with urllib.request.urlopen(retry, timeout=90) as response:
            return response.read()


def raw_github_url(url: str) -> str:
    match = re.match(r"https://github\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.*)", url)
    if match:
        owner, repo, ref, path = match.groups()
        return f"https://raw.githubusercontent.com/{owner}/{repo}/{ref}/{path}"
    return url


def chunks(values: list[str], size: int = BATCH_SIZE) -> Iterable[list[str]]:
    for i in range(0, len(values), size):
        yield values[i:i + size]


def wiki_url(title: str) -> str:
    return FANDOM_BASE + urllib.parse.quote(title.replace(" ", "_"), safe="'()_-,")


def normalize_name(value: str) -> str:
    value = value.lower()
    value = value.replace("’", "'").replace("‘", "'")
    value = re.sub(r"\([^)]*\)", " ", value)
    value = re.sub(r"[\"'`´]", "", value)
    value = value.replace("&", " and ")
    value = re.sub(r"[^a-z0-9]+", " ", value)
    return " ".join(value.split())


def canonical_without_nickname(value: str) -> str:
    value = re.sub(r"([\"']).*?\1", " ", value)
    return normalize_name(value)


def safe_id(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9._-]+", "_", value).strip("_")


def match_player_page(canonical_name: str, discovered: dict[str, dict[str, Any]]):
    by_normalized = {normalize_name(title): data for title, data in discovered.items()}

    direct = by_normalized.get(normalize_name(canonical_name))
    if direct:
        return direct, "exact"

    for alias in ALIASES.get(normalize_name(canonical_name), []):
        direct = by_normalized.get(normalize_name(alias))
        if direct:
            return direct, "alias"

    canonical_short = canonical_without_nickname(canonical_name)
    for title, data in discovered.items():
        if canonical_without_nickname(title) == canonical_short:
            return data, "nickname-normalized"

    target = normalize_name(canonical_name)
    scored = []
    for title, data in discovered.items():
        score = difflib.SequenceMatcher(None, target, normalize_name(title)).ratio()
        scored.append((score, title, data))
    scored.sort(reverse=True, key=lambda row: row[0])
    if scored and scored[0][0] >= 0.84:
        return scored[0][2], f"fuzzy:{scored[0][0]:.2f}"

    return None, None


def parse_all_players_links() -> list[str]:
    data = api({"action": "parse", "page": FANDOM_INDEX_PAGE, "prop": "links"})
    parse = data.get("parse") or {}
    links = parse.get("links") or []

    titles = []
    seen = set()
    for link in links:
        ns = link.get("ns", 0)
        title = (link.get("title") or link.get("*") or "").strip()
        if ns != 0 or not title or title in seen:
            continue
        if title in {"Main Page", "Blood Bowl", "All Players"}:
            continue
        seen.add(title)
        titles.append(title)
    return titles


def categories_for_titles(titles: list[str]) -> dict[str, list[str]]:
    result = {}
    for batch in chunks(titles):
        data = api({
            "action": "query",
            "titles": "|".join(batch),
            "prop": "categories|info",
            "cllimit": "max",
            "inprop": "url",
            "redirects": 1,
        })
        for page in (data.get("query") or {}).get("pages") or []:
            title = page.get("title")
            if not title or page.get("missing"):
                continue
            categories = [
                (cat.get("title") or "").removeprefix("Category:")
                for cat in page.get("categories") or []
            ]
            result[title] = categories
    return result


def looks_like_player_page(title: str, categories: list[str]) -> bool:
    category_text = " ".join(categories).lower()
    if "player" in category_text:
        return True

    bad_words = (
        "team", "race", "league", "competition", "rule", "skill", "staff",
        "stadium", "sponsor", "magazine", "book", "edition", "timeline",
    )
    lowered = title.lower()
    return not any(word in lowered for word in bad_words)


def discover_player_pages() -> dict[str, dict[str, Any]]:
    linked_titles = parse_all_players_links()
    categories = categories_for_titles(linked_titles)

    discovered = {}
    for title in linked_titles:
        cats = categories.get(title)
        canonical_title = title
        if cats is None:
            normalized = normalize_name(title)
            for candidate, candidate_cats in categories.items():
                if normalize_name(candidate) == normalized:
                    canonical_title = candidate
                    cats = candidate_cats
                    break
        cats = cats or []
        if looks_like_player_page(canonical_title, cats):
            discovered[canonical_title] = {
                "title": canonical_title,
                "pageUrl": wiki_url(canonical_title),
                "categories": cats,
            }
    return discovered


def fetch_details(titles: list[str]) -> dict[str, dict[str, Any]]:
    details = {}

    for batch in chunks(titles):
        data = api({
            "action": "query",
            "titles": "|".join(batch),
            "prop": "info|extracts|pageimages|revisions",
            "inprop": "url",
            "exintro": 1,
            "explaintext": 1,
            "piprop": "original|thumbnail",
            "pithumbsize": 1400,
            "rvslots": "main",
            "rvprop": "content",
            "redirects": 1,
        })

        for page in (data.get("query") or {}).get("pages") or []:
            title = page.get("title")
            if not title or page.get("missing"):
                continue

            revision_content = ""
            revisions = page.get("revisions") or []
            if revisions:
                slots = revisions[0].get("slots") or {}
                main = slots.get("main") or {}
                revision_content = (
                    main.get("content")
                    or main.get("*")
                    or revisions[0].get("content")
                    or revisions[0].get("*")
                    or ""
                )

            original = page.get("original") or {}
            thumbnail = page.get("thumbnail") or {}

            details[title] = {
                "title": title,
                "pageUrl": page.get("fullurl") or wiki_url(title),
                "extract": (page.get("extract") or "").strip(),
                "imageUrl": original.get("source") or thumbnail.get("source"),
                "wikitext": revision_content,
            }

    return details


def strip_wiki_markup(value: str) -> str:
    value = re.sub(r"<!--.*?-->", " ", value, flags=re.S)
    value = re.sub(r"<ref\b[^>]*>.*?</ref>", " ", value, flags=re.S | re.I)
    value = re.sub(r"<ref\b[^>]*/>", " ", value, flags=re.I)
    value = re.sub(r"<br\s*/?>", ", ", value, flags=re.I)
    value = re.sub(r"\[\[(?:[^]|]+\|)?([^]]+)\]\]", r"\1", value)
    value = re.sub(r"\{\{[^{}]*\}\}", " ", value)
    value = re.sub(r"<[^>]+>", " ", value)
    value = value.replace("'''", "").replace("''", "")
    return " ".join(value.split())


def linked_names(value: str) -> list[str]:
    names = []
    for target, label in re.findall(r"\[\[([^]|]+)(?:\|([^]]+))?\]\]", value):
        name = (label or target).strip()
        if name and name not in names:
            names.append(name)
    return names


TEAM_FIELD_RE = re.compile(
    r"^\|\s*(?:team|teams|club|clubs|current[_ ]?team|former[_ ]?teams?"
    r"|teams?[_ ]?played[_ ]?for|played[_ ]?for|career[_ ]?teams?)\s*=\s*(.+)$",
    flags=re.I,
)


def extract_team_history(wikitext: str) -> list[str]:
    teams = []

    for line in wikitext.splitlines():
        match = TEAM_FIELD_RE.match(line.strip())
        if not match:
            continue
        raw = match.group(1)
        candidates = linked_names(raw)
        if not candidates:
            cleaned = strip_wiki_markup(raw)
            candidates = [p.strip() for p in re.split(r"[,;/]", cleaned) if p.strip()]
        for team in candidates:
            if team.lower() not in {"unknown", "n/a", "none", "various"} and team not in teams:
                teams.append(team)

    section_re = re.compile(
        r"(?ms)^==+\s*(?:teams?|career|playing career|history)\s*==+\s*(.*?)(?=^==+|\Z)",
        flags=re.I,
    )
    for section in section_re.findall(wikitext):
        for team in linked_names(section):
            if team not in teams:
                teams.append(team)

    return teams[:30]


def estimate_background(image: Image.Image, sample_size: int = 16):
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


def is_foreground(pixel, background):
    r, g, b, a = pixel
    if a <= 24:
        return False
    distance = abs(r - background[0]) + abs(g - background[1]) + abs(b - background[2])
    return distance > 36 or a < 250


def trim_to_subject(image: Image.Image, margin_ratio: float = 0.04) -> Image.Image:
    width, height = image.size
    bg = estimate_background(image)
    pixels = image.load()
    left, top, right, bottom = width, height, -1, -1

    for y in range(height):
        for x in range(width):
            if is_foreground(pixels[x, y], bg):
                left = min(left, x)
                top = min(top, y)
                right = max(right, x)
                bottom = max(bottom, y)

    if right < left or bottom < top:
        return image

    pad = max(2, int(round(max(width, height) * margin_ratio)))
    return image.crop((
        max(0, left - pad),
        max(0, top - pad),
        min(width, right + pad + 1),
        min(height, bottom + pad + 1),
    ))


def detect_base_cut(image: Image.Image) -> int | None:
    width, height = image.size
    bg = estimate_background(image)
    pixels = image.load()

    coverage = []
    dark_fraction = []
    mean_luma = []

    for y in range(height):
        fg = dark = 0
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
        coverage.append(fg / width if width else 0.0)
        dark_fraction.append(dark / fg if fg else 0.0)
        mean_luma.append(luma_sum / fg if fg else 255.0)

    for y in range(int(height * 0.68), int(height * 0.95)):
        cov_now = sum(coverage[y:y + 3]) / max(1, len(coverage[y:y + 3]))
        dark_now = sum(dark_fraction[y:y + 3]) / max(1, len(dark_fraction[y:y + 3]))
        luma_now = sum(mean_luma[y:y + 3]) / max(1, len(mean_luma[y:y + 3]))
        prev_from = max(0, y - 8)
        cov_prev = sum(coverage[prev_from:y]) / max(1, y - prev_from)
        if cov_now >= 0.42 and dark_now >= 0.28 and luma_now <= 125 and cov_now >= cov_prev + 0.07:
            cut = max(int(height * 0.60), y - max(2, int(round(height * 0.01))))
            if cut >= int(height * 0.70):
                return cut
    return None


def original_extension(data: bytes, url: str | None = None) -> str:
    try:
        with Image.open(io.BytesIO(data)) as image:
            fmt = (image.format or "").upper()
        return {
            "JPEG": "jpg",
            "PNG": "png",
            "WEBP": "webp",
            "GIF": "gif",
            "BMP": "bmp",
        }.get(fmt, "img")
    except Exception:
        if url:
            suffix = Path(urllib.parse.urlparse(url).path).suffix.lower().lstrip(".")
            if suffix and re.fullmatch(r"[a-z0-9]{2,5}", suffix):
                return suffix
        return "img"


def normalize_fandom_or_web_image(data: bytes):
    with Image.open(io.BytesIO(data)) as image:
        image.load()
        image = image.convert("RGBA")
        image.thumbnail((MAX_DIMENSION, MAX_DIMENSION), Image.Resampling.LANCZOS)
        width, height = image.size
        output = io.BytesIO()
        image.save(output, "PNG", optimize=True)
        return output.getvalue(), width, height


def normalize_bbbase_image(data: bytes, keep_bases: bool = False):
    with Image.open(io.BytesIO(data)) as image:
        image.load()
        image = image.convert("RGBA")
        image = trim_to_subject(image)
        if not keep_bases:
            cut = detect_base_cut(image)
            if cut is not None:
                image = image.crop((0, 0, image.width, cut))
            image = trim_to_subject(image, margin_ratio=0.03)
        image.thumbnail((MAX_DIMENSION, MAX_DIMENSION), Image.Resampling.LANCZOS)
        width, height = image.size
        output = io.BytesIO()
        image.save(output, "PNG", optimize=True)
        return output.getvalue(), width, height


def normalize_for_source(source_kind: str, data: bytes, keep_bases: bool = False):
    if source_kind == "bbbase":
        return normalize_bbbase_image(data, keep_bases=keep_bases)
    return normalize_fandom_or_web_image(data)


@dataclass
class Candidate:
    player_name: str
    source_kind: str
    title: str
    url: str
    page_url: str | None
    width: int | None = None
    height: int | None = None
    score: float = 0.0
    notes: list[str] | None = None
    downloaded_as: str | None = None
    selected: bool = False


def _int_or_none(value):
    try:
        return int(value)
    except Exception:
        return None


def score_candidate(candidate: Candidate) -> Candidate:
    score = 0.0
    notes: list[str] = []

    if candidate.source_kind == "fandom":
        score += 70.0
        notes.append("primary-known-source")
    elif candidate.source_kind == "bbbase":
        score += 45.0
        notes.append("bbbase-fallback")
    else:
        score += 40.0

    normalized_name = normalize_name(candidate.player_name)
    title_norm = normalize_name(candidate.title)
    page_norm = normalize_name(candidate.page_url or "")
    if normalized_name and normalized_name in title_norm:
        score += 14.0
        notes.append("title-match")
    elif normalized_name and normalized_name in page_norm:
        score += 10.0
        notes.append("page-match")

    text_blob = f"{candidate.title} {candidate.page_url or ''} {candidate.url}".lower()
    if "blood bowl" in text_blob:
        score += 8.0
        notes.append("blood-bowl-keyword")
    if "card" in text_blob or "legend" in text_blob or "star player" in text_blob:
        score += 5.0
        notes.append("card-or-star-keyword")
    if "miniature" in text_blob:
        score += 2.0
        notes.append("miniature-keyword")

    if candidate.width and candidate.height:
        area = candidate.width * candidate.height
        if area >= 1_000_000:
            score += 10.0
            notes.append("large-image")
        elif area >= 300_000:
            score += 6.0
            notes.append("medium-image")
        elif area < 80_000:
            score -= 8.0
            notes.append("small-image")

    if candidate.url.lower().endswith(".gif"):
        score -= 15.0
        notes.append("gif-penalty")

    candidate.score = round(score, 2)
    candidate.notes = notes
    return candidate


def choose_candidates(candidates: list[Candidate]) -> list[Candidate]:
    if not candidates:
        return []
    ranked = sorted((score_candidate(c) for c in candidates), key=lambda c: c.score, reverse=True)
    best = ranked[0].score
    return [c for c in ranked if c.score >= best - NEAR_EQUAL_MARGIN][:MAX_DOWNLOADED_CANDIDATES]


def save_original(candidate_dir: Path, candidate: Candidate, index: int, data: bytes) -> Path:
    ext = original_extension(data, candidate.url)
    prefix = f"{index:02d}_{candidate.source_kind}"
    slug = safe_id(candidate.title or candidate.player_name) or "candidate"
    path = candidate_dir / f"{prefix}_{slug}.{ext}"
    path.write_bytes(data)
    candidate.downloaded_as = path.name
    return path


def compose_star_profile(player, fandom, teams, bbbase_markdown):
    lines = [f"# {player['name']}", ""]

    if fandom:
        lines.extend([
            "## Historical / literature context",
            "",
            f"Source: {fandom['pageUrl']}",
            "",
        ])
        if fandom.get("extract"):
            lines.extend([fandom["extract"].strip(), ""])
        if teams:
            lines.extend(["### Teams / clubs mentioned", ""])
            lines.extend(f"- {team}" for team in teams)
            lines.append("")

    if bbbase_markdown:
        lines.extend([
            "## Current BBBase rules/profile snapshot",
            "",
            bbbase_markdown.strip(),
            "",
        ])

    if not fandom and not bbbase_markdown:
        lines.extend(["No source profile text was available.", ""])

    return "\n".join(lines).strip() + "\n"


def context_metadata(metadata_dir: Path, stem: str) -> Path:
    return metadata_dir / f"{stem}.json"


def write_active_reference(
    references_dir: Path,
    metadata_dir: Path,
    stem: str,
    player_name: str,
    source_kind: str,
    source_url: str,
    source_path: Path,
    data: bytes,
    keep_bases: bool = False,
) -> tuple[int, int]:
    encoded, width, height = normalize_for_source(source_kind, data, keep_bases=keep_bases)
    out = references_dir / f"{stem}.png"
    out.write_bytes(encoded)
    context_metadata(metadata_dir, stem).write_text(
        json.dumps({
            "player": player_name,
            "sourceKind": source_kind,
            "sourceUrl": source_url,
            "sourcePath": str(source_path),
            "generatedAt": now_iso(),
            "width": width,
            "height": height,
        }, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return width, height


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, default=CATALOG)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--keep-bases", action="store_true")
    parser.add_argument("--no-history-details", action="store_true")
    args = parser.parse_args()

    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    output = args.output_dir.resolve()
    references = output / "references"
    reference_meta = output / "reference-meta"
    profiles = output / "profiles"
    candidates_root = output / "candidates"
    references.mkdir(parents=True, exist_ok=True)
    reference_meta.mkdir(parents=True, exist_ok=True)
    profiles.mkdir(parents=True, exist_ok=True)
    candidates_root.mkdir(parents=True, exist_ok=True)

    print("Discovering player pages from Fandom All_Players ...")
    discovered = discover_player_pages()
    print(f"Discovered {len(discovered)} probable player pages.")

    details = {}
    if not args.no_history_details:
        print("Fetching historical player details in batches ...")
        details = fetch_details(list(discovered.keys()))

    historical = {
        "generatedAt": now_iso(),
        "source": wiki_url(FANDOM_INDEX_PAGE),
        "players": {},
    }

    for title, base in discovered.items():
        detail = details.get(title) or {}
        wikitext = detail.get("wikitext") or ""
        historical["players"][title] = {
            "name": title,
            "pageUrl": detail.get("pageUrl") or base["pageUrl"],
            "categories": base.get("categories") or [],
            "summary": detail.get("extract") or None,
            "teams": extract_team_history(wikitext) if wikitext else [],
            "imageUrl": detail.get("imageUrl") or None,
        }

    (output / "historical-players.json").write_text(
        json.dumps(historical, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    manifest = {
        "generatedAt": now_iso(),
        "sourceCommit": catalog.get("sourceCommit"),
        "maxDimension": MAX_DIMENSION,
        "keepBases": args.keep_bases,
        "primarySource": wiki_url(FANDOM_INDEX_PAGE),
        "historicalPlayers": len(discovered),
        "players": {},
    }

    failures = []
    matched_count = 0
    used_counts: dict[str, int] = {"fandom": 0, "bbbase": 0, "other": 0}

    players = catalog.get("players", [])
    for index, player in enumerate(players, 1):
        player_id = player["id"]
        stem = safe_id(player_id)
        candidate_dir = candidates_root / stem
        profile_path = profiles / f"{stem}.md"

        try:
            if args.force and candidate_dir.exists():
                shutil.rmtree(candidate_dir)
            candidate_dir.mkdir(parents=True, exist_ok=True)

            matched, match_method = match_player_page(player["name"], discovered)
            fandom_detail = None
            teams = []

            if matched:
                matched_count += 1
                title = matched["title"]
                fandom_detail = details.get(title)
                if fandom_detail is None:
                    fandom_detail = fetch_details([title]).get(title)
                if fandom_detail:
                    teams = extract_team_history(fandom_detail.get("wikitext") or "")

            bbbase_markdown = None
            try:
                bbbase_markdown = download(raw_github_url(player["sourceUrl"])).decode("utf-8")
            except Exception as exc:
                print(f"  WARN {player['name']}: BBBase profile fetch failed: {exc}", file=sys.stderr)

            if args.force or not profile_path.exists():
                profile_path.write_text(
                    compose_star_profile(player, fandom_detail, teams, bbbase_markdown),
                    encoding="utf-8",
                )

            candidates: list[Candidate] = []

            if fandom_detail and fandom_detail.get("imageUrl"):
                candidates.append(Candidate(
                    player_name=player["name"],
                    source_kind="fandom",
                    title=fandom_detail.get("title") or player["name"],
                    url=fandom_detail["imageUrl"],
                    page_url=fandom_detail.get("pageUrl"),
                    notes=["fandom-page-image"],
                ))

            if player.get("imageUrl"):
                candidates.append(Candidate(
                    player_name=player["name"],
                    source_kind="bbbase",
                    title=player["name"],
                    url=player["imageUrl"],
                    page_url=player.get("sourceUrl"),
                    notes=["bbbase-image"],
                ))

            picked = choose_candidates(candidates)
            if not picked:
                raise RuntimeError("No image candidates available")

            downloaded = []
            selected_width = selected_height = None
            selected_source_kind = None
            selected_source_url = None
            selected_original_path = None

            for rank, candidate in enumerate(picked, 1):
                try:
                    blob = download(candidate.url, candidate.page_url if candidate.source_kind == "fandom" else None)
                    original_path = save_original(candidate_dir, candidate, rank, blob)
                    downloaded.append(candidate)

                    if selected_original_path is None:
                        width, height = write_active_reference(
                            references, reference_meta, stem, player["name"],
                            candidate.source_kind, candidate.url, original_path, blob,
                            keep_bases=args.keep_bases,
                        )
                        candidate.selected = True
                        selected_width, selected_height = width, height
                        selected_source_kind = candidate.source_kind
                        selected_source_url = candidate.url
                        selected_original_path = original_path
                        used_counts[candidate.source_kind if candidate.source_kind in used_counts else "other"] += 1
                except Exception as exc:
                    print(f"  WARN {player['name']}: candidate download failed for {candidate.url}: {exc}", file=sys.stderr)

            if selected_original_path is None:
                raise RuntimeError("All image candidates failed to download or process")

            (candidate_dir / "metadata.json").write_text(
                json.dumps({
                    "player": player["name"],
                    "playerId": player_id,
                    "generatedAt": now_iso(),
                    "selectedOriginal": selected_original_path.name if selected_original_path else None,
                    "candidates": [asdict(c) for c in downloaded],
                }, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )

            manifest["players"][player_id] = {
                "name": player["name"],
                "reference": f"references/{stem}.png",
                "referenceMeta": f"reference-meta/{stem}.json",
                "profile": f"profiles/{profile_path.name}",
                "candidateDir": f"candidates/{stem}",
                "width": selected_width,
                "height": selected_height,
                "fandomTitle": fandom_detail.get("title") if fandom_detail else None,
                "fandomPage": fandom_detail.get("pageUrl") if fandom_detail else None,
                "fandomMatchMethod": match_method,
                "teams": teams,
                "sourceUrl": player.get("sourceUrl"),
                "selectedSourceUrl": selected_source_url,
                "selectedSourceKind": selected_source_kind,
                "downloadedCandidates": len(downloaded),
            }

            print(
                f"[{index:3}/{len(players):3}] {player['name']}: "
                f"{selected_width}x{selected_height} "
                f"({selected_source_kind}; wiki={match_method or 'unmatched'}; "
                f"candidates={len(downloaded)})"
            )

        except Exception as exc:
            failures.append((player_id, str(exc)))
            print(f"FAILED {player['name']}: {exc}", file=sys.stderr)

    (output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"\nAssets: {output}")
    print(f"Historical player pages: {len(discovered)}")
    print(f"Star Players matched to All_Players: {matched_count}")
    print("Active context images chosen from:")
    for kind, count in used_counts.items():
        print(f"  {kind}: {count}")
    print(f"Star Players synced: {len(manifest['players'])}")

    if failures:
        print(f"Failed: {len(failures)}", file=sys.stderr)
        for player_id, error in failures:
            print(f"  {player_id}: {error}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
