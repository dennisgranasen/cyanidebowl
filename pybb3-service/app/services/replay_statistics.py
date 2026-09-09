"""Canonical action aggregation for Blood Bowl replay statistics.

Decoders reconstruct actions first. Statistics are then game-version agnostic:
D6 values are successful actions / attempted actions, while block statistics
expose both selected faces and the resolved action consequence.
"""
from __future__ import annotations

from collections import defaultdict
from typing import Any, Iterable

from .replay_actions import BLOCK_FACE_NAMES, BlockAction, BlockOutcome, D6Action, SpecialAction
from .replay_decoders import Bb3ActionDecoder

BLOCK_FACE_ORDER = tuple(BLOCK_FACE_NAMES.values())
BLOCK_OUTCOME_ORDER = (
    BlockOutcome.SUCCESS.value,
    BlockOutcome.NEUTRAL.value,
    BlockOutcome.FAILURE.value,
    BlockOutcome.UNKNOWN.value,
)


def aggregate_actions(actions: Iterable[D6Action | BlockAction | SpecialAction]) -> list[dict[str, Any]]:
    """Return tidy, API-friendly per-team action statistics."""
    d6: dict[tuple[str, int, Any, int | None, str | None], dict[str, Any]] = {}
    faces: dict[tuple[str, Any], dict[str, Any]] = {}
    outcomes: dict[tuple[str, Any], dict[str, Any]] = {}
    specials: dict[tuple[str, Any], dict[str, Any]] = {}

    for action in actions:
        if isinstance(action, D6Action):
            key = (
                action.action_type,
                action.target,
                action.team_id,
                action.source_roll_type,
                action.roll_category,
            )
            row = d6.setdefault(
                key,
                {
                    "eventType": action.action_type,
                    "kind": "d6",
                    "target": f"{action.target}+",
                    "teamId": action.team_id,
                    "success": 0,
                    "total": 0,
                    "sourceRollType": action.source_roll_type,
                    "rollCategory": action.roll_category,
                },
            )
            row["total"] += 1
            row["success"] += int(action.successful)
            continue

        if isinstance(action, BlockAction):
            face_key = (action.dice_label, action.attacker_team_id)
            face_row = faces.setdefault(
                face_key,
                {
                    "eventType": "Block",
                    "kind": "blockFaces",
                    "target": action.dice_label,
                    "teamId": action.attacker_team_id,
                    "total": 0,
                    **{face: 0 for face in BLOCK_FACE_ORDER},
                    "unknown": 0,
                },
            )
            selected = action.selected_face if action.selected_face in BLOCK_FACE_ORDER else "unknown"
            face_row[selected] += 1
            face_row["total"] += 1

            outcome_key = (action.dice_label, action.attacker_team_id)
            outcome_row = outcomes.setdefault(
                outcome_key,
                {
                    "eventType": "Block outcome",
                    "kind": "blockActions",
                    "target": action.dice_label,
                    "teamId": action.attacker_team_id,
                    "success": 0,
                    "neutral": 0,
                    "fail": 0,
                    "unknown": 0,
                    "total": 0,
                },
            )
            outcome_row[action.outcome.value] += 1
            outcome_row["total"] += 1
            continue

        if isinstance(action, SpecialAction):
            special_key = (action.action_type, action.team_id)
            special_row = specials.setdefault(
                special_key,
                {
                    "eventType": action.action_type,
                    "kind": "special",
                    "target": "All",
                    "teamId": action.team_id,
                    "success": 0,
                    "neutral": 0,
                    "fail": 0,
                    "unknown": 0,
                    "total": 0,
                },
            )
            special_row[action.outcome.value] += 1
            special_row["total"] += 1

    return sorted(
        [*d6.values(), *faces.values(), *outcomes.values(), *specials.values()],
        key=lambda row: (row["eventType"], row["target"], str(row["teamId"])),
    )


def event_statistics(root) -> list[dict[str, Any]]:
    """Backward-compatible pivot used by the current frontend/API.

    The source of truth is canonical actions. `actionStatistics` can use the
    tidy form directly; this pivot can be removed once callers migrate.
    """
    tidy = aggregate_actions(Bb3ActionDecoder().decode(root))
    grouped: dict[tuple[str, str, str], list[dict[str, Any]]] = defaultdict(list)
    for row in tidy:
        team = {"teamIndex": row["teamId"]}
        team.update({key: value for key, value in row.items() if key not in {"eventType", "kind", "target", "teamId"}})
        grouped[(row["eventType"], row["kind"], row["target"])].append(team)
    return [
        {
            "eventType": event_type,
            "kind": kind,
            "target": target,
            "teams": sorted(teams, key=lambda value: str(value["teamIndex"])),
        }
        for (event_type, kind, target), teams in sorted(grouped.items())
    ]
