from pathlib import Path
import sys

replacements = {
    Path("backend/src/main/java/net/warp_scores/warpscores/domain/persistence/MatchRepository.java"): [
        (
            "import org.springframework.data.domain.Pageable;\n",
            "import org.springframework.data.domain.Pageable;\n"
            "import org.springframework.data.domain.Slice;\n",
        ),
        (
            """    @Query("{ '_id.opus': 3, 'finished': { '$ne': null }, 'matchId': { '$nin': [null, ''] } }")
    List<Match> findReplayAdminMatches(Pageable pageable);
""",
            """    @Query("{ '_id.type': 'SimpleIdentity', '_id.value': { '$regex': '^3-' }, "
            + "'finished': { '$ne': null }, 'matchId': { '$nin': [null, ''] } }")
    Slice<Match> findReplayAdminMatches(Pageable pageable);
""",
        ),
    ],
    Path("backend/src/main/java/net/warp_scores/warpscores/controller/ReplaySweeperAdminController.java"): [
        (
            """        // One extra row tells the UI whether an older page exists without a count query.
        var recent = matches.findReplayAdminMatches(PageRequest.of(
                safePage,
                safeSize + 1,
                Sort.by(Sort.Direction.DESC, "finished")));
        boolean hasMore = recent.size() > safeSize;
        var pageMatches = hasMore ? recent.subList(0, safeSize) : recent;
""",
            """        var recent = matches.findReplayAdminMatches(PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "finished")));
        boolean hasMore = recent.hasNext();
        var pageMatches = recent.getContent();
""",
        ),
    ],
}

for path, changes in replacements.items():
    if not path.exists():
        sys.exit(f"Missing file: {path}")
    text = path.read_text(encoding="utf-8")
    original = text
    for old, new in changes:
        count = text.count(old)
        if count != 1:
            sys.exit(f"Expected exactly one match in {path}, found {count}. No files were written.")
        text = text.replace(old, new, 1)
    if text == original:
        sys.exit(f"No changes made to {path}")

# Only write after every replacement has been validated.
for path, changes in replacements.items():
    text = path.read_text(encoding="utf-8")
    for old, new in changes:
        text = text.replace(old, new, 1)
    path.write_text(text, encoding="utf-8")
    print(f"Updated {path}")
