from __future__ import annotations

import gzip
import base64
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from collections import Counter
from typing import Any

from app.services.bb3_die_types import bb3_dice_semantics, bb3_die_name, infer_bb3_die_type
from app.services.bb3_roll_types import Bb3RollType, bb3_roll_name
from app.services.replay_decoders import Bb2ReplayDecoder, Bb3ActionDecoder, action_dicts, decode_message
from app.services.replay_statistics import aggregate_actions, event_statistics
from app.services.replay_timeline import build_replay_timeline
from app.services.replay_player_identity import build_player_index

PARSER_VERSION = 19
INTEGER = re.compile(r"^-?(?:0|[1-9][0-9]*)$")
RESOURCE_MARKERS = ("reroll", "apothec", "wizard", "spell")
SPECIAL_MARKERS = (
    "regener", "resurrect", "raise", "bribe", "arguethecall", "secretweapon",
    "bomb", "chainsaw", "vomit", "bloodlust", "hypnotic",
)
ROLLOFF_ROLL_TYPES = {
    Bb3RollType.BrilliantCoaching,
    Bb3RollType.CheeringFans,
    Bb3RollType.OfficiousRefRollOff,
    Bb3RollType.SeismicActivityRollOff,
}


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


def _decoded_text(element: ET.Element, path: str) -> str | None:
    value = _text(element, path)
    if not isinstance(value, str):
        return None
    try:
        return base64.b64decode(value, validate=True).decode("utf-8")
    except (ValueError, UnicodeDecodeError):
        return value


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


def _event_team(event, context):
    # Match-wide events must never inherit whichever team happened to be
    # active in the surrounding board state.
    if event.tag in {"EventWeatherRoll", "EventKickOffTable"}:
        return None
    team = _first(event, ("TeamId", "GamerSlot", "GamerId"))
    return context.get("activeTeam") if team is None else team


def _injury_player_id(
    event: ET.Element,
    label: str | None,
    inherited_player_id: int | None,
    inherited_target_player_id: int | None,
) -> int | None:
    """Resolve the player whose armour/injury/casualty state is being rolled."""
    explicit = _first(
        event,
        ("InjuredPlayerId", "KnockedOutPlayerId", "VictimId", "TargetId", "DefenderId", "PlayerId"),
    )
    if isinstance(explicit, int):
        return explicit
    if label == "Regeneration" and isinstance(inherited_player_id, int):
        return inherited_player_id
    return inherited_target_player_id if isinstance(inherited_target_player_id, int) else None


def _fact(event: ET.Element, sequence: int, clock: Any, context: dict[str, Any], data: Any) -> dict[str, Any]:
    fact = {
        "sequence": sequence, "clock": clock, "eventType": event.tag,
        "teamId": _event_team(event, context),
        "playerId": _first(event, ("PlayerId", "ActivePlayer", "AttackerId", "ThrowerId")),
        "phase": context.get("phase"), "teamTurns": context.get("teamTurns", []),
        "outcome": _first(event, ("Outcome", "Result")), "data": data,
    }
    success = _success(event)
    if success is not None:
        fact["success"] = success
    return fact


def _dice(
    event: ET.Element,
    sequence: int,
    clock: Any,
    context: dict[str, Any],
    *,
    source: str = "event",
    inherited_player_id: int | None = None,
    inherited_target_player_id: int | None = None,
) -> list[dict[str, Any]]:
    result = []
    groups = list(event.iter("Dice"))
    roll_type = _text(event, ".//RollType")
    roll_name = bb3_roll_name(roll_type) if isinstance(roll_type, int) else None
    category, label = bb3_dice_semantics(event.tag, roll_type, roll_name)
    for roll_index, group in enumerate(groups):
        dice = []
        for die in group.findall(".//Die"):
            value = _text(die, "./Value")
            if value is not None:
                source_type = _text(die, "./DieType")
                resolved_type, type_source = infer_bb3_die_type(event.tag, roll_type, source_type)
                dice.append({
                    "sourceType": source_type,
                    "type": resolved_type,
                    "typeName": bb3_die_name(resolved_type),
                    "typeSource": type_source,
                    "value": value,
                })
        if not dice:
            continue
        modifiers = []
        for modifier in event.findall(".//Modifier"):
            modifiers.append({"type": _text(modifier, "./ModifierType"), "value": _text(modifier, "./Value")})
        player_id = _first(event, ("PlayerId", "ActivePlayer", "AttackerId", "ThrowerId"))
        explicit_team_id = _first(event, ("TeamId", "GamerSlot", "GamerId"))
        team_id = _event_team(event, context)
        if category == "injury":
            player_id = _injury_player_id(
                event, label, inherited_player_id, inherited_target_player_id
            )
            team_id = context.get("playerTeams", {}).get(player_id) if player_id is not None else None
        if roll_type == Bb3RollType.ArgueTheCall and explicit_team_id is None:
            # Argue the Call belongs to the coach of the acting/sent-off
            # player. ActiveTeam may already have advanced to the opponent.
            owner_player_id = player_id if player_id is not None else inherited_player_id
            owner_team_id = context.get("playerTeams", {}).get(owner_player_id)
            if owner_team_id is not None:
                team_id = owner_team_id
        # EventFanFactor contains HomeRoll and AwayRoll as separate Dice groups.
        if event.tag == "EventFanFactor" and len(groups) == 2:
            team_id = roll_index

        # Kick-off roll-offs are match-wide contests: one D6 belongs to each
        # team. They must not inherit ActiveTeam from the surrounding board.
        # BB3 has been observed both with two Dice groups and with two dice in
        # one Dice group, so preserve the source group while assigning the
        # individual team rolls explicitly.
        team_dice = [(team_id, dice)]
        if roll_type in ROLLOFF_ROLL_TYPES and explicit_team_id is None:
            if len(groups) == 2:
                team_dice = [(roll_index, dice)]
            elif len(groups) == 1 and len(dice) == 2:
                team_dice = [(index, [die]) for index, die in enumerate(dice)]
            else:
                # An incomplete/unknown roll-off shape is safer as match-wide
                # than falsely charging it to whichever team is active.
                team_dice = [(None, dice)]

        for resolved_team_id, resolved_dice in team_dice:
            result.append({
            "sequence": sequence, "clock": clock, "source": source,
            "eventType": event.tag, "contextTag": event.tag, "rollIndex": roll_index,
            "rollType": roll_type, "rollTypeName": roll_name,
            "category": category, "label": label,
            "outcome": _text(event, ".//Outcome"),
            "playerId": player_id,
            "teamId": resolved_team_id,
            "success": _success(event), "phase": context.get("phase"),
            "teamTurns": context.get("teamTurns", []), "dice": resolved_dice, "modifiers": modifiers,
            })
    return result


def _decoded_sequence_dice(event: ET.Element, sequence: int, clock: Any, context: dict[str, Any]) -> list[dict[str, Any]]:
    """Preserve dice hidden inside base64 encoded BB3 StepResult messages."""
    result: list[dict[str, Any]] = []
    for step_result in event.findall(".//Sequence/StepResult"):
        step = decode_message(step_result.find("Step"))
        step_player_id = _first(
            step, ("PlayerId", "ActivePlayer", "AttackerId", "ThrowerId")
        ) if step is not None else None
        step_target_id = _first(
            step, ("TargetId", "DefenderId", "VictimId", "ReceiverId")
        ) if step is not None else None
        if step is not None:
            result.extend(_dice(
                step, sequence, clock, context, source="decoded-step",
                inherited_player_id=step_player_id if isinstance(step_player_id, int) else None,
                inherited_target_player_id=step_target_id if isinstance(step_target_id, int) else None,
            ))
        for wrapper in step_result.findall("Results/StringMessage"):
            decoded = decode_message(wrapper)
            if decoded is not None:
                result.extend(_dice(
                    decoded, sequence, clock, context, source="decoded-result",
                    inherited_player_id=step_player_id if isinstance(step_player_id, int) else None,
                    inherited_target_player_id=step_target_id if isinstance(step_target_id, int) else None,
                ))
    return result


def _semantic_results(label: str | None, die_type: int | None, values: list[Any]) -> list[int]:
    """Derive display results while retaining the original component dice."""
    numeric = [value for value in values if isinstance(value, int)]
    if die_type == 0 and label in {"Armour", "Injury", "Weather", "Kick-off Table"}:
        if len(numeric) == 2:
            return [sum(numeric)]
        if len(numeric) > 2 and len(numeric) % 2 == 0:
            return [sum(numeric[index:index + 2]) for index in range(0, len(numeric), 2)]
    # D16 casualty values are individual outcomes. Never add multiple D16
    # values together; a group may contain an original and replacement roll.
    return numeric


def _dice_statistics(rolls: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Aggregate semantic dice groups while retaining their individual rolls."""
    grouped: dict[tuple[Any, ...], dict[str, Any]] = {}
    for roll in rolls:
        dice = roll.get("dice") or []
        resolved_types = {die.get("type") for die in dice if die.get("type") is not None}
        die_type = next(iter(resolved_types)) if len(resolved_types) == 1 else None
        key = (roll.get("category"), roll.get("label"), die_type, roll.get("teamId"), roll.get("rollType"))
        row = grouped.setdefault(key, {
            "category": roll.get("category"), "label": roll.get("label"),
            "dieType": die_type, "dieTypeName": bb3_die_name(die_type),
            "teamId": roll.get("teamId"), "rollType": roll.get("rollType"),
            "rollTypeName": roll.get("rollTypeName"), "rollCount": 0, "dieCount": 0,
            "inferred": False, "faceCounts": {}, "resultCounts": {}, "rolls": [], "sources": [],
        })
        values = [die.get("value") for die in dice if die.get("value") is not None]
        row["rollCount"] += 1
        row["dieCount"] += len(values)
        row["rolls"].append(values)
        row["sources"].append({
            "sequence": roll.get("sequence"), "clock": roll.get("clock"),
            "source": roll.get("source"), "eventType": roll.get("eventType"),
            "playerId": roll.get("playerId"),
        })
        for result in _semantic_results(roll.get("label"), die_type, values):
            result_key = str(result)
            row["resultCounts"][result_key] = row["resultCounts"].get(result_key, 0) + 1
        for die in dice:
            if die.get("typeSource") == "context":
                row["inferred"] = True
            value = die.get("value")
            if value is not None:
                face = str(value)
                row["faceCounts"][face] = row["faceCounts"].get(face, 0) + 1
    return sorted(grouped.values(), key=lambda row: (
        str(row.get("category")), str(row.get("label")), str(row.get("teamId")), str(row.get("dieTypeName"))
    ))


def parse_replay(xml: bytes, source_format: str = "BB3") -> dict[str, Any]:
    if b"<!DOCTYPE" in xml.upper():
        raise ValueError("Replay XML must not contain a document type declaration")
    root = ET.fromstring(xml)
    if root.tag != "Replay":
        raise ValueError(f"Expected Replay root, found {root.tag}")

    player_index = build_player_index(root)
    player_teams = {
        player_id: identity.get("teamId")
        for player_id, identity in player_index.items()
        if identity.get("teamId") is not None
    }

    compact: dict[str, Any] = {
        "format": "BLASKSCORE_REPLAY", "formatVersion": 2,
        "sourceFormat": source_format,
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
        context["playerTeams"] = player_teams
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
            if source_format == "BB3" and event.tag == "EventExecuteSequence":
                rolls.extend(_decoded_sequence_dice(event, sequence, clock, context))
            dice_rolls.extend(rolls)
            for roll in rolls:
                for die in roll["dice"]:
                    # D6 is protocol value 0, so never use truthiness here.
                    die_type = die.get("type")
                    die_counts[f"{die_type if die_type is not None else 'UNKNOWN'}:{die['value']}"] += 1
            lowered = event.tag.lower()
            fact = _fact(event, sequence, clock, context, data)
            if any(marker in lowered for marker in RESOURCE_MARKERS):
                resources.append(fact)
            if any(marker in lowered for marker in SPECIAL_MARKERS):
                special.append(fact)
        compact["steps"].append(step)

    compact["finalBoardState"] = final_board
    checkpoint_count = sum("checkpoint" in step for step in compact["steps"])
    decoder = Bb3ActionDecoder() if source_format == "BB3" else Bb2ReplayDecoder()
    actions = decoder.decode(root)
    action_stats = aggregate_actions(actions)
    canonical_actions = action_dicts(actions)
    timeline = build_replay_timeline(xml) if source_format == "BB3" else None
    analysis = {
        "diceStatistics": _dice_statistics(dice_rolls),
        "eventStatistics": event_statistics(root) if source_format == "BB3" else [],
        "actionStatistics": action_stats,
        "canonicalActions": canonical_actions,
        "timeline": timeline,
        # Legacy protocol-derived matchEvents are no longer produced for BB3.
        "matchEvents": [],
        "weatherEvents": [],
        "parserVersion": PARSER_VERSION, "replayVersion": compact["replayVersion"],
        "sourceFormat": source_format,
        "analysisConfidence": "CANONICAL_ACTIONS" if source_format == "BB3" else "RAW_BB2",
        "sourceMatchId": _decoded_text(root, ".//NotificationGameJoined/GameInfos/Competition/CompetitionInfos/MatchId"),
        "stepCount": len(compact["steps"]), "eventCount": sum(event_counts.values()),
        "sourceBoardStateCount": source_board_count, "checkpointCount": checkpoint_count,
        "diceRolls": dice_rolls, "resourceEvents": resources, "specialEvents": special,
        "eventTypeCounts": dict(sorted(event_counts.items())), "dieValueCounts": dict(sorted(die_counts.items())),
    }
    compact["canonicalActions"] = analysis["canonicalActions"]
    compact_bytes = json.dumps(compact, ensure_ascii=False, separators=(",", ":")).encode()
    return {
        "analysis": analysis,
        "originalGzip": gzip.compress(xml, compresslevel=9),
        "compactGzip": gzip.compress(compact_bytes, compresslevel=9),
        "originalSha256": hashlib.sha256(xml).hexdigest(),
        "compactSha256": hashlib.sha256(compact_bytes).hexdigest(),
    }


def parse_replay_artifact(data: bytes) -> tuple[str, bytes, dict[str, Any]]:
    """Parse either BB3 XML/BBR-decoded XML or a BB2 .bbrz archive."""
    if data.startswith(b"PK\x03\x04"):
        xml = Bb2ReplayDecoder.extract_xml(data)
        return "BBRZ", xml, parse_replay(xml, source_format="BB2")
    return "BBR", data, parse_replay(data, source_format="BB3")
