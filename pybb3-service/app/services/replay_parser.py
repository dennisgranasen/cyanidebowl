from __future__ import annotations

import gzip
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from collections import Counter
from typing import Any

PARSER_VERSION = 1
INTEGER = re.compile(r"^-?(?:0|[1-9][0-9]*)$")
RESOURCE_MARKERS = ("reroll", "apothec", "wizard", "spell")
SPECIAL_MARKERS = (
    "regener", "resurrect", "raise", "bribe", "arguethecall", "secretweapon",
    "bomb", "chainsaw", "vomit", "bloodlust", "hypnotic",
)


def _scalar(text: str | None) -> str | int | None:
    if text is None or not text.strip():
        return None
    value = text.strip()
    return int(value) if INTEGER.fullmatch(value) else value


def _value(element: ET.Element) -> Any:
    children = list(element)
    if not children:
        return _scalar(element.text)
    result: dict[str, Any] = {}
    for child in children:
        value = _value(child)
        if child.tag not in result:
            result[child.tag] = value
        elif isinstance(result[child.tag], list):
            result[child.tag].append(value)
        else:
            result[child.tag] = [result[child.tag], value]
    return result


def _add(target: dict[str, Any], tag: str, value: Any) -> None:
    if tag not in target:
        target[tag] = value
    elif isinstance(target[tag], list):
        target[tag].append(value)
    else:
        target[tag] = [target[tag], value]


def _text(element: ET.Element, path: str) -> Any:
    found = element.find(path)
    return _scalar(found.text) if found is not None else None


def _board_context(board: ET.Element | None) -> dict[str, Any]:
    if board is None:
        return {}
    teams = []
    for index, team in enumerate(board.findall("./ListTeams/TeamState")):
        team_id = _text(team, "./Data/TeamId")
        if team_id is None:
            team_id = _text(team, "./TeamId")
        teams.append({"teamId": index if team_id is None else team_id, "gameTurn": _text(team, "./GameTurn")})
    return {
        "phase": _text(board, "./CurrentPhase"),
        "activeTeam": _text(board, "./ActiveTeam"),
        "activePlayer": _text(board, "./ActivePlayer"),
        "teamTurns": teams,
    }


def _turn_signature(context: dict[str, Any]) -> tuple[Any, ...]:
    return (
        context.get("phase"), context.get("activeTeam"),
        tuple((team.get("teamId"), team.get("gameTurn")) for team in context.get("teamTurns", [])),
    )


def _first(event: ET.Element, names: tuple[str, ...]) -> Any:
    for name in names:
        value = _text(event, f".//{name}")
        if value is not None:
            return value
    return None


def _success(event: ET.Element) -> bool | None:
    value = _first(event, ("Success", "Successful", "IsSuccess", "Succeeded"))
    if isinstance(value, int) and value in (0, 1):
        return bool(value)
    if isinstance(value, str) and value.lower() in ("true", "false"):
        return value.lower() == "true"
    return None


def _fact(event: ET.Element, sequence: int, clock: Any, context: dict[str, Any], data: Any) -> dict[str, Any]:
    fact = {
        "sequence": sequence, "clock": clock, "eventType": event.tag,
        "teamId": _first(event, ("TeamId", "GamerSlot", "GamerId")) or context.get("activeTeam"),
        "playerId": _first(event, ("PlayerId", "ActivePlayer", "AttackerId", "ThrowerId")),
        "phase": context.get("phase"), "teamTurns": context.get("teamTurns", []),
        "outcome": _first(event, ("Outcome", "Result")), "data": data,
    }
    success = _success(event)
    if success is not None:
        fact["success"] = success
    return fact


def _dice(event: ET.Element, sequence: int, clock: Any, context: dict[str, Any]) -> list[dict[str, Any]]:
    result = []
    for roll_index, group in enumerate(event.iter("Dice")):
        dice = []
        for die in group.findall(".//Die"):
            value = _text(die, "./Value")
            if value is not None:
                dice.append({"type": _text(die, "./DieType"), "value": value})
        if not dice:
            continue
        modifiers = []
        for modifier in event.findall(".//Modifier"):
            modifiers.append({"type": _text(modifier, "./ModifierType"), "value": _text(modifier, "./Value")})
        result.append({
            "sequence": sequence, "clock": clock, "eventType": event.tag, "rollIndex": roll_index,
            "rollType": _text(event, ".//RollType"), "outcome": _text(event, ".//Outcome"),
            "playerId": _first(event, ("PlayerId", "ActivePlayer", "AttackerId", "ThrowerId")),
            "teamId": _first(event, ("TeamId", "GamerSlot", "GamerId")) or context.get("activeTeam"),
            "success": _success(event), "phase": context.get("phase"),
            "teamTurns": context.get("teamTurns", []), "dice": dice, "modifiers": modifiers,
        })
    return result


def parse_replay(xml: bytes) -> dict[str, Any]:
    if b"<!DOCTYPE" in xml.upper():
        raise ValueError("Replay XML must not contain a document type declaration")
    root = ET.fromstring(xml)
    if root.tag != "Replay":
        raise ValueError(f"Expected Replay root, found {root.tag}")

    compact: dict[str, Any] = {
        "format": "BLASKSCORE_REPLAY", "formatVersion": 1,
        "replayVersion": _text(root, "./ReplayVersion"), "header": {}, "steps": [],
    }
    dice_rolls: list[dict[str, Any]] = []
    resources: list[dict[str, Any]] = []
    special: list[dict[str, Any]] = []
    event_counts: Counter[str] = Counter()
    die_counts: Counter[str] = Counter()
    previous_signature: tuple[Any, ...] | None = None
    final_board: Any = None
    source_board_count = 0

    for child in root:
        if child.tag != "ReplayStep":
            _add(compact["header"], child.tag, _value(child))
            continue
        sequence = len(compact["steps"])
        clock = _text(child, "./Clock")
        board = child.find("./BoardState")
        context = _board_context(board)
        signature = _turn_signature(context)
        step: dict[str, Any] = {"sequence": sequence, "clock": clock, "events": []}
        if board is not None:
            source_board_count += 1
            final_board = _value(board)
            if previous_signature is None or signature != previous_signature:
                step["checkpoint"] = {"reason": "TURN_OR_PHASE_CHANGE", "context": context, "boardState": final_board}
            previous_signature = signature

        for event in child:
            if event.tag in ("Clock", "BoardState"):
                continue
            data = _value(event)
            step["events"].append({"type": event.tag, "data": data})
            event_counts[event.tag] += 1
            rolls = _dice(event, sequence, clock, context)
            dice_rolls.extend(rolls)
            for roll in rolls:
                for die in roll["dice"]:
                    die_counts[f"{die['type'] or 'UNKNOWN'}:{die['value']}"] += 1
            lowered = event.tag.lower()
            fact = _fact(event, sequence, clock, context, data)
            if any(marker in lowered for marker in RESOURCE_MARKERS):
                resources.append(fact)
            if any(marker in lowered for marker in SPECIAL_MARKERS):
                special.append(fact)
        compact["steps"].append(step)

    compact["finalBoardState"] = final_board
    checkpoint_count = sum("checkpoint" in step for step in compact["steps"])
    analysis = {
        "parserVersion": PARSER_VERSION, "replayVersion": compact["replayVersion"],
        "stepCount": len(compact["steps"]), "eventCount": sum(event_counts.values()),
        "sourceBoardStateCount": source_board_count, "checkpointCount": checkpoint_count,
        "diceRolls": dice_rolls, "resourceEvents": resources, "specialEvents": special,
        "eventTypeCounts": dict(sorted(event_counts.items())), "dieValueCounts": dict(sorted(die_counts.items())),
    }
    compact_bytes = json.dumps(compact, ensure_ascii=False, separators=(",", ":")).encode()
    return {
        "analysis": analysis,
        "originalGzip": gzip.compress(xml, compresslevel=9),
        "compactGzip": gzip.compress(compact_bytes, compresslevel=9),
        "originalSha256": hashlib.sha256(xml).hexdigest(),
        "compactSha256": hashlib.sha256(compact_bytes).hexdigest(),
    }
