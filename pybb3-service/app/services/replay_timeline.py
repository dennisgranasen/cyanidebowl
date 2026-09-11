"""Canonical high-level match timeline for Blood Bowl replay analysis.

This layer is intentionally presentation-oriented but lossless: timeline items
only reference explicit replay evidence and keep raw event tags/sequence
positions as provenance. Low-level dice/actions remain in their existing
analysis collections.

BB2 must eventually produce the same MatchEvent shape from its own decoder;
this module currently interprets BB3 event/tag conventions only.
"""
from __future__ import annotations

import base64
import re
import xml.etree.ElementTree as ET
from typing import Any, Iterable

from bb3.replay import Replay
from app.services.replay_decoders import Bb3ActionDecoder, action_dicts, decode_message
from app.services.replay_player_identity import build_player_index, enrich_match_event

INTEGER = re.compile(r"^-?(?:0|[1-9][0-9]*)$")

SPP_BY_TYPE = {
    "TOUCHDOWN": 3,
    "COMPLETION": 1,
    "INTERCEPTION": 2,
    "CASUALTY": 2,
    "MVP": 4,
}

CONSEQUENCE_TYPES = {"CASUALTY", "INJURY", "KO", "DEATH"}
SELF_CAUSE_ACTIONS = {
    "dodge", "rush", "gfi", "jumpover", "jump", "leap", "landing", "land",
}
ACTION_ID = re.compile(r"^bb3:(\d+):(\d+):")


def _action_position(action: dict[str, Any]) -> tuple[int, int] | None:
    action_id = action.get("actionId")
    if not isinstance(action_id, str):
        return None
    match = ACTION_ID.match(action_id)
    if match is None:
        return None
    return int(match.group(1)), int(match.group(2))


def _action_actor(action: dict[str, Any]) -> int | None:
    for key in ("attackerPlayerId", "playerId"):
        value = action.get(key)
        if isinstance(value, int):
            return value
    return None


def _action_target(action: dict[str, Any]) -> int | None:
    for key in ("defenderPlayerId", "targetPlayerId", "targetId"):
        value = action.get(key)
        if isinstance(value, int) and value >= 0:
            return value
    return None


def _action_label(action: dict[str, Any]) -> str:
    value = action.get("actionType") or action.get("kind") or "Action"
    label = str(value).strip()
    return label[:1].upper() + label[1:] if label else "Action"


def _self_causing_action(action: dict[str, Any], affected_player_id: int | None) -> bool:
    if affected_player_id is None or _action_actor(action) != affected_player_id:
        return False
    successful = action.get("successful")
    if successful is not False:
        return False
    token = re.sub(r"[^a-z0-9]", "", _action_label(action).lower())
    return token in SELF_CAUSE_ACTIONS


def _find_source_action(
    canonical_actions: list[dict[str, Any]],
    replay_sequence: int,
    step_result_index: int | None,
    affected_player_id: int | None,
) -> tuple[dict[str, Any] | None, bool]:
    """Find the strongest causal action immediately preceding a consequence.

    Targeted actions are preferred. A failed self-action is accepted only for
    known injury-capable actions such as Dodge/Rush/Jump/Landing. Pure temporal
    proximity alone is not enough to invent a causing player.
    """
    if affected_player_id is None:
        return None, False

    candidates: list[tuple[int, dict[str, Any]]] = []
    for action in canonical_actions:
        position = _action_position(action)
        if position is None or position[0] != replay_sequence:
            continue
        action_index = position[1]
        if step_result_index is not None and action_index >= step_result_index:
            continue
        candidates.append((action_index, action))

    for _, action in sorted(candidates, key=lambda item: item[0], reverse=True):
        if _action_target(action) == affected_player_id:
            return action, False
        if _self_causing_action(action, affected_player_id):
            return action, True

    return None, False


def _scalar(text: str | None) -> str | int | None:
    if text is None or not text.strip():
        return None
    value = text.strip()
    return int(value) if INTEGER.fullmatch(value) else value


def _text(element: ET.Element | None, path: str) -> Any:
    if element is None:
        return None
    found = element.find(path)
    return _scalar(found.text) if found is not None else None


def _first(element: ET.Element, names: tuple[str, ...]) -> Any:
    for name in names:
        value = _text(element, f".//{name}")
        if value is not None:
            return value
    return None


def _bool(element: ET.Element, names: tuple[str, ...]) -> bool | None:
    value = _first(element, names)
    if isinstance(value, int) and value in (0, 1):
        return bool(value)
    if isinstance(value, str) and value.lower() in {"true", "false"}:
        return value.lower() == "true"
    return None


def _dice_values(element: ET.Element) -> list[int]:
    values: list[int] = []
    for die in element.findall(".//Dice//Die"):
        value = _text(die, "./Value")
        if isinstance(value, int):
            values.append(value)
    return values


def _modifiers(element: ET.Element) -> list[dict[str, Any]]:
    result = []
    for modifier in element.findall(".//Modifier"):
        result.append({
            "type": _text(modifier, "./ModifierType"),
            "value": _text(modifier, "./Value"),
        })
    return result


def _modifier_total(modifiers: list[dict[str, Any]]) -> int:
    return sum(value for value in (modifier.get("value") for modifier in modifiers) if isinstance(value, int))


def _standard_weather(total: int | None) -> str | None:
    if total == 2:
        return "Sweltering Heat"
    if total == 3:
        return "Very Sunny"
    if isinstance(total, int) and 4 <= total <= 10:
        return "Perfect Conditions"
    if total == 11:
        return "Pouring Rain"
    if total == 12:
        return "Blizzard"
    return None


def _board_context(step: ET.Element) -> dict[str, Any]:
    board = step.find("./BoardState")
    if board is None:
        return {}
    active_team = _text(board, "./ActiveTeam")
    team_turns = []
    for index, team in enumerate(board.findall("./ListTeams/TeamState")):
        team_id = _text(team, "./Data/TeamId")
        if team_id is None:
            team_id = _text(team, "./TeamId")
        team_turns.append({
            "teamId": index if team_id is None else team_id,
            "turn": _text(team, "./GameTurn"),
        })
    turn = None
    for team in team_turns:
        if team.get("teamId") == active_team:
            turn = team.get("turn")
            break
    return {
        "phase": _text(board, "./CurrentPhase"),
        "half": _text(board, "./CurrentHalf") or _text(board, "./Half"),
        "activeTeam": active_team,
        "turn": turn,
        "teamTurns": team_turns,
    }


def _iter_events(root: ET.Element) -> Iterable[tuple[int, Any, dict[str, Any], int, ET.Element, str]]:
    """Yield raw and decoded BB3 events with stable replay provenance."""
    for sequence, step in enumerate(root.findall("./ReplayStep")):
        clock = _text(step, "./Clock")
        context = _board_context(step)
        event_index = 0
        for event in step:
            if event.tag in {"Clock", "BoardState"}:
                continue
            yield sequence, clock, context, event_index, event, "event"
            event_index += 1
            if event.tag != "EventExecuteSequence":
                continue
            for step_result_index, step_result in enumerate(event.findall(".//Sequence/StepResult")):
                decoded_step = decode_message(step_result.find("Step"))
                step_context = dict(context)
                step_context["stepResultIndex"] = step_result_index
                if decoded_step is not None:
                    step_context["stepPlayerId"] = _first(
                        decoded_step, ("PlayerId", "ActivePlayer", "AttackerId", "ThrowerId")
                    )
                    step_context["stepTargetId"] = _first(
                        decoded_step, ("TargetId", "DefenderId", "VictimId", "ReceiverId")
                    )
                    step_context["stepActionType"] = decoded_step.tag
                    yield sequence, clock, step_context, event_index, decoded_step, "decoded-step"
                    event_index += 1
                for wrapper in step_result.findall("Results/StringMessage"):
                    decoded = decode_message(wrapper)
                    if decoded is not None:
                        yield sequence, clock, step_context, event_index, decoded, "decoded-result"
                        event_index += 1


def _classify(event: ET.Element) -> tuple[str | None, str | None]:
    tag = event.tag.lower()
    if "touchdown" in tag or "touch_down" in tag:
        return "TOUCHDOWN", "Touchdown"
    if "completion" in tag or "completedpass" in tag or "passcompleted" in tag:
        return "COMPLETION", "Completion"
    if "interception" in tag:
        return "INTERCEPTION", "Interception"
    if "casualty" in tag:
        return "CASUALTY", "Casualty"
    if "knockout" in tag or "knockedout" in tag or "knocked_out" in tag:
        return "KO", "Knock-out"
    if "injury" in tag:
        return "INJURY", "Injury"
    if "death" in tag or "killed" in tag:
        return "DEATH", "Death"
    if "eject" in tag or "sentoff" in tag or "sendoff" in tag or "expulsion" in tag:
        return "EJECTION", "Ejection"
    if "apothec" in tag:
        return "APOTHECARY", "Apothecary"
    if "mvp" in tag:
        return "MVP", "MVP"
    if "weather" in tag:
        return "WEATHER", "Weather"
    if "brilliantcoaching" in tag or "brilliant_coaching" in tag:
        return "KICKOFF_DETAIL", "Brilliant Coaching"
    if "cheeringfans" in tag or "cheering_fans" in tag:
        return "KICKOFF_DETAIL", "Cheering Fans"
    if "kickoff" in tag or "kick_off" in tag:
        return "KICKOFF", "Kick-off"
    return None, None


def _actor_player(event: ET.Element, event_type: str, context: dict[str, Any]) -> Any:
    if event_type == "CASUALTY":
        # DamageStep.PlayerId identifies the injured player in real BB3
        # replays, so do not use it as a causal fallback.
        return _first(event, ("CausingPlayerId", "AttackerId", "BlockerId"))
    if event_type == "COMPLETION":
        return _first(event, ("ThrowerId", "PlayerId", "ActivePlayer")) or context.get("stepPlayerId")
    if event_type in {"INJURY", "KO", "DEATH", "APOTHECARY"}:
        return _first(event, ("CausingPlayerId", "AttackerId"))
    return _first(
        event, ("PlayerId", "ScorerId", "IntercepterId", "InterceptorId", "ActivePlayer")
    ) or context.get("stepPlayerId")


def _affected_player(event: ET.Element, event_type: str, context: dict[str, Any]) -> Any:
    if event_type == "CASUALTY":
        return _first(
            event, ("VictimId", "InjuredPlayerId", "TargetId", "DefenderId", "PlayerId")
        ) or context.get("stepTargetId")
    if event_type == "COMPLETION":
        return _first(event, ("ReceiverId", "CatcherId", "TargetId")) or context.get("stepTargetId")
    if event_type in {"INJURY", "KO", "DEATH", "APOTHECARY"}:
        return _first(
            event,
            ("InjuredPlayerId", "KnockedOutPlayerId", "VictimId", "TargetId", "DefenderId", "PlayerId"),
        ) or context.get("stepTargetId")
    return None


def _team(event: ET.Element) -> Any:
    return _first(event, ("TeamId", "GamerSlot", "ScoringTeamId", "KickingTeamId", "RollingTeamId"))


def _weather_details(event: ET.Element) -> dict[str, Any]:
    dice = _dice_values(event)
    modifiers = _modifiers(event)
    raw_total = sum(dice) if dice else None
    modified_total = raw_total + _modifier_total(modifiers) if raw_total is not None else None
    table_id = _first(event, ("WeatherTableId", "TableId"))
    table_name = _first(event, ("WeatherTableName", "TableName"))
    default_standard = table_id in (None, 0, "0") and not table_name
    if default_standard:
        table_name = "Standard"
    weather = _first(event, ("WeatherName", "Weather", "ResultName"))
    if weather is None and (default_standard or str(table_name).lower() == "standard"):
        weather = _standard_weather(modified_total)
    return {
        "tableId": table_id,
        "tableName": table_name,
        "tableInferred": default_standard,
        "dice": dice,
        "rawTotal": raw_total,
        "modifiers": modifiers,
        "modifiedTotal": modified_total,
        "weather": weather,
    }


def _kickoff_details(event: ET.Element) -> dict[str, Any]:
    dice = _dice_values(event)
    modifiers = _modifiers(event)
    raw_total = sum(dice) if dice else None
    modified_total = raw_total + _modifier_total(modifiers) if raw_total is not None else None
    return {
        "kickingTeamId": _first(event, ("KickingTeamId", "KickerTeamId")),
        "receivingTeamId": _first(event, ("ReceivingTeamId", "ReceiverTeamId")),
        "dice": dice,
        "rawTotal": raw_total,
        "modifiers": modifiers,
        "modifiedTotal": modified_total,
        "resultId": _first(event, ("KickOffResult", "KickoffResult", "Result", "Outcome", "EventId")),
        "resultName": _first(event, ("KickOffEventName", "KickoffEventName", "ResultName", "EventName", "Name")),
    }


class ReplayTimelineError(ValueError):
    """Raised when pybb3 cannot produce its canonical narrative timeline."""


def build_replay_timeline(xml_content: bytes) -> dict[str, Any]:
    """Return pybb3's canonical compact narrative timeline.

    This deliberately mirrors tools/replay_timeline.py --narrative in pybb3.
    BlaskScore must not maintain a second protocol-level timeline parser.
    """
    try:
        timeline = Replay.from_xml(xml_content).timeline().to_narrative_dict()
    except Exception as exc:
        raise ReplayTimelineError("pybb3 could not build the replay narrative timeline") from exc

    if not isinstance(timeline, dict):
        raise ReplayTimelineError("pybb3 returned a non-object narrative timeline")
    if timeline.get("format") != "pybb3-narrative-timeline":
        raise ReplayTimelineError(
            f"Unexpected pybb3 narrative timeline format: {timeline.get('format')!r}"
        )
    return timeline


def build_match_events(root: ET.Element, canonical_actions: list[dict[str, Any]] | None = None) -> list[dict[str, Any]]:
    """Build a chronological, conservative high-level event timeline."""
    events: list[dict[str, Any]] = []
    drive = 0
    latest_kickoff: dict[str, Any] | None = None
    pending_apothecary: dict[str, Any] | None = None
    score = [0, 0]
    player_index = build_player_index(root)
    if canonical_actions is None:
        canonical_actions = action_dicts(Bb3ActionDecoder().decode(root))

    for sequence, clock, context, event_index, event, source in _iter_events(root):
        tag = event.tag.lower()

        # BB3 records an apothecary as a three-part chain:
        # usage question -> casualty choice -> ResultApothecary.
        # Only the final result belongs on the timeline; the question messages
        # carry the original/new casualty rolls needed to explain the choice.
        if tag == "questionapothecarycasualtyusage":
            pending_apothecary = {
                "affectedPlayerId": context.get("stepTargetId") or context.get("stepPlayerId"),
                "originalCasualtyResult": _first(event, ("Outcome",)),
                "originalCasualtyDice": _dice_values(event),
            }
            continue
        if tag == "questionapothecarycasualtychoice":
            pending_apothecary = dict(pending_apothecary or {})
            original = event.find(".//OriginalRoll")
            new_roll = event.find(".//NewRoll")
            if original is not None:
                pending_apothecary["originalCasualtyResult"] = _first(original, ("Outcome",))
                pending_apothecary["originalCasualtyDice"] = _dice_values(original)
            if new_roll is not None:
                pending_apothecary["apothecaryRerollResult"] = _first(new_roll, ("Outcome",))
                pending_apothecary["apothecaryRerollDice"] = _dice_values(new_roll)
            pending_apothecary.setdefault(
                "affectedPlayerId", context.get("stepTargetId") or context.get("stepPlayerId")
            )
            continue

        event_type, title = _classify(event)
        if event_type is None:
            continue

        if event_type == "KICKOFF":
            drive += 1

        team_id = _team(event)
        player_id = _actor_player(event, event_type, context)
        affected_player_id = _affected_player(event, event_type, context)
        details: dict[str, Any] = {}
        if event_type == "WEATHER":
            details = _weather_details(event)
            # Weather is match state, never owned by the active team.
            team_id = None
        elif event_type == "KICKOFF":
            details = _kickoff_details(event)
            team_id = details.get("kickingTeamId")
        else:
            dice = _dice_values(event)
            if dice:
                details["dice"] = dice
            result = _first(event, ("ResultName", "Result", "Outcome"))
            if result is not None:
                details["result"] = result

        if event_type == "APOTHECARY":
            details.update(pending_apothecary or {})
            chosen = _first(event, ("Casualty",))
            if chosen is not None:
                details["chosenCasualtyResult"] = chosen
            if isinstance(details.get("affectedPlayerId"), int):
                affected_player_id = details["affectedPlayerId"]
            # Apothecary has a treated player, not an acting/targeting player.
            player_id = None

        source_action = None
        self_inflicted = False
        if event_type in CONSEQUENCE_TYPES and isinstance(affected_player_id, int):
            source_action, self_inflicted = _find_source_action(
                canonical_actions,
                sequence,
                context.get("stepResultIndex"),
                affected_player_id,
            )
            if source_action is not None:
                linked_actor = _action_actor(source_action)
                # Explicit replay causation wins; otherwise use the linked action.
                if player_id is None or player_id == affected_player_id:
                    player_id = linked_actor
                linked_identity = player_index.get(linked_actor) if isinstance(linked_actor, int) else None
                if linked_identity and linked_identity.get("name"):
                    details["causingPlayerName"] = linked_identity["name"]
                details["sourceActionId"] = source_action.get("actionId")
                details["sourceActionType"] = _action_label(source_action)
                details["causeType"] = "SELF" if self_inflicted else _action_label(source_action).upper()
                details["causeConfidence"] = "HIGH"
                details["selfInflicted"] = self_inflicted

        event_id = f"m{sequence}-{event_index}-{event_type.lower()}"
        item = {
            "id": event_id,
            "type": event_type,
            "title": title,
            "sequence": sequence,
            "eventIndex": event_index,
            "clock": clock,
            "source": source,
            "rawEventType": event.tag,
            "half": context.get("half"),
            "drive": drive or None,
            "turn": context.get("turn"),
            "activeTeamId": context.get("activeTeam"),
            "teamId": team_id,
            "playerId": player_id,
            "details": details,
        }
        enrich_match_event(
            item,
            event,
            event_type,
            player_index,
            actor_player_id=player_id if isinstance(player_id, int) else None,
            affected_player_id=affected_player_id if isinstance(affected_player_id, int) else None,
        )
        team_id = item.get("teamId")

        if event_type == "TOUCHDOWN" and isinstance(team_id, int) and team_id in (0, 1):
            score[team_id] += 1
            item["score"] = {"home": score[0], "away": score[1]}

        spp = SPP_BY_TYPE.get(event_type)
        casualty_has_causer = (
            event_type != "CASUALTY"
            or (
                isinstance(player_id, int)
                and player_id != affected_player_id
                and not details.get("selfInflicted")
            )
        )
        if spp is not None and player_id is not None and casualty_has_causer:
            item["sppAwarded"] = spp
            item["sppReason"] = event_type

        if event_type == "APOTHECARY":
            pending_apothecary = None

        # Resolution rolls/events directly following a kick-off belong to that
        # kick-off chain. The relation is provenance, not destructive nesting.
        if event_type in {"KICKOFF_DETAIL", "WEATHER"} and latest_kickoff is not None:
            if sequence - int(latest_kickoff["sequence"]) <= 3:
                item["parentEventId"] = latest_kickoff["id"]

        events.append(item)
        if event_type == "KICKOFF":
            latest_kickoff = item

    return events
