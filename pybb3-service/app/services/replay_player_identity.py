"""Resolve BB3 runtime player ids to roster identities.

BB3 BoardState PlayerState ids are runtime ids used by decoded replay events.
The corresponding PlayerState/Data/LobbyId links back to the roster PlayerData
entry in NotificationGameJoined. Player and coach identities are deliberately
kept separate; timeline enrichment only exposes player names here.
"""
from __future__ import annotations

import base64
import xml.etree.ElementTree as ET
from typing import Any


def _text(element: ET.Element | None, path: str) -> str | None:
    if element is None:
        return None
    found = element.find(path)
    if found is None or found.text is None:
        return None
    value = found.text.strip()
    return value or None


def _int(element: ET.Element | None, path: str) -> int | None:
    value = _text(element, path)
    if value is None:
        return None
    try:
        return int(value)
    except ValueError:
        return None


def _decode_text(value: str | None) -> str | None:
    """Decode BB3 base64 text while preserving unexpected plain text."""
    if value is None:
        return None
    try:
        decoded = base64.b64decode(value, validate=True).decode("utf-8").strip()
        return decoded or value
    except (ValueError, UnicodeDecodeError):
        return value


def _roster_by_lobby_id(root: ET.Element) -> dict[str, dict[str, Any]]:
    """Index roster players by the stable LobbyId embedded in the replay."""
    result: dict[str, dict[str, Any]] = {}
    gamers = root.findall(".//NotificationGameJoined/GameInfos/GamersInfos/GamerInfos")
    for gamer_index, gamer in enumerate(gamers):
        slot = _int(gamer, "./Slot")
        team_id = gamer_index if slot is None else slot
        for player in gamer.findall("./Roster/Players/PlayerData"):
            lobby_id = _text(player, "./LobbyId")
            if lobby_id is None:
                continue
            result[lobby_id] = {
                "lobbyId": _decode_text(lobby_id),
                "name": _decode_text(_text(player, "./Name")),
                "number": _int(player, "./Number"),
                "teamId": team_id,
            }
    return result


def build_player_index(root: ET.Element) -> dict[int, dict[str, Any]]:
    """Map BB3 runtime PlayerState/Id values to roster player identities."""
    roster = _roster_by_lobby_id(root)
    result: dict[int, dict[str, Any]] = {}

    for replay_step in root.findall("./ReplayStep"):
        board = replay_step.find("./BoardState")
        if board is None:
            continue
        for team_index, team in enumerate(board.findall("./ListTeams/TeamState")):
            for state in team.findall("./ListPitchPlayers/PlayerState"):
                player_id = _int(state, "./Id")
                if player_id is None:
                    continue

                lobby_id = _text(state, "./Data/LobbyId")
                roster_player = roster.get(lobby_id or "", {})
                identity = {
                    "playerId": player_id,
                    "teamId": roster_player.get("teamId", team_index),
                    "lobbyId": roster_player.get("lobbyId") or _decode_text(lobby_id),
                    "name": roster_player.get("name") or _decode_text(_text(state, "./Data/Name")),
                    "number": roster_player.get("number") or _int(state, "./Data/Number"),
                }

                # Board states repeat throughout the replay. Later states may
                # contain fields that were absent earlier, so merge non-null
                # values instead of replacing a richer identity.
                previous = result.get(player_id, {})
                result[player_id] = {
                    key: value
                    for key, value in {**previous, **identity}.items()
                    if value is not None
                }

    return result


def _first_int(event: ET.Element, names: tuple[str, ...]) -> int | None:
    for name in names:
        value = event.findtext(f".//{name}")
        if value is None:
            continue
        try:
            return int(value)
        except ValueError:
            continue
    return None


def _target_player_id(event: ET.Element, event_type: str) -> int | None:
    if event_type == "CASUALTY":
        return _first_int(
            event,
            ("VictimId", "InjuredPlayerId", "TargetId", "DefenderId", "PlayerId"),
        )
    if event_type == "COMPLETION":
        return _first_int(event, ("ReceiverId", "CatcherId", "TargetId"))
    if event_type in {"INJURY", "DEATH", "APOTHECARY"}:
        return _first_int(event, ("InjuredPlayerId", "VictimId", "TargetId", "PlayerId"))
    return None


def enrich_match_event(
    item: dict[str, Any],
    raw_event: ET.Element,
    event_type: str,
    players: dict[int, dict[str, Any]],
) -> None:
    """Attach player identities and explicit event roles to a timeline item."""
    player_id = item.get("playerId")
    actor = players.get(player_id) if isinstance(player_id, int) else None

    if actor:
        if item.get("teamId") is None and actor.get("teamId") is not None:
            item["teamId"] = actor["teamId"]
        if actor.get("name"):
            item["playerName"] = actor["name"]
        if actor.get("number") is not None:
            item["playerNumber"] = actor["number"]

    target_id = _target_player_id(raw_event, event_type)
    target = players.get(target_id) if target_id is not None else None
    details = item.setdefault("details", {})

    if target_id is not None:
        details["targetPlayerId"] = target_id
    if target and target.get("name"):
        details["targetPlayerName"] = target["name"]
    if target and target.get("number") is not None:
        details["targetPlayerNumber"] = target["number"]

    actor_name = actor.get("name") if actor else None
    target_name = target.get("name") if target else None

    if event_type == "TOUCHDOWN" and actor_name:
        details["scorerName"] = actor_name
    elif event_type == "COMPLETION":
        if actor_name:
            details["throwerName"] = actor_name
        if target_name:
            details["receiverName"] = target_name
    elif event_type == "INTERCEPTION" and actor_name:
        details["interceptorName"] = actor_name
    elif event_type == "CASUALTY":
        if actor_name:
            details["causingPlayerName"] = actor_name
        if target_name:
            details["injuredPlayerName"] = target_name
    elif event_type in {"INJURY", "DEATH", "APOTHECARY"}:
        # For these result events PlayerId commonly identifies the affected
        # player rather than the player who caused the preceding action.
        affected = target_name or actor_name
        if affected:
            details["injuredPlayerName"] = affected
    elif actor_name:
        details["playerName"] = actor_name
