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
    """Map BB3 numeric replay player ids to roster/player identities.

    Injury/result PlayerId values such as 37 and 43 correspond to the numeric
    ids stored in PlayerState/Id and/or PlayerState/Data/Id. Full metadata can
    live in InitialBoardState even when later BoardState snapshots are sparse.

    Scan every ListTeams/TeamState tree in the replay and merge identities from
    PlayerState, PlayerData and LobbyId.
    """
    roster_by_lobby = _roster_by_lobby_id(root)
    roster_by_id: dict[int, dict[str, Any]] = {}
    result: dict[int, dict[str, Any]] = {}

    for player in root.findall(".//PlayerData"):
        player_id = _int(player, "./Id")
        if player_id is None:
            continue
        lobby_raw = _text(player, "./LobbyId")
        roster_by_id[player_id] = {
            "lobbyId": _decode_text(lobby_raw),
            "name": _decode_text(_text(player, "./Name")),
            "number": _int(player, "./Number"),
        }

    def merge(player_id: int | None, identity: dict[str, Any]) -> None:
        if player_id is None:
            return
        previous = result.get(player_id, {})
        result[player_id] = {
            key: value
            for key, value in {**previous, **identity}.items()
            if value is not None
        }

    # Includes NotificationGameJoined/InitialBoardState, ReplayStep/BoardState,
    # and any end-game board state carrying player information.
    for list_teams in root.findall(".//ListTeams"):
        for team_index, team in enumerate(list_teams.findall("./TeamState")):
            for state in team.findall(".//PlayerState"):
                state_id = _int(state, "./Id")
                data_id = _int(state, "./Data/Id")
                numeric_id = data_id if data_id is not None else state_id

                lobby_raw = _text(state, "./Data/LobbyId")
                lobby_player = roster_by_lobby.get(lobby_raw or "", {})
                id_player = roster_by_id.get(numeric_id or -1, {})

                lobby_id = (
                    lobby_player.get("lobbyId")
                    or _decode_text(lobby_raw)
                    or id_player.get("lobbyId")
                )
                identity = {
                    "teamId": team_index,
                    "lobbyId": lobby_id,
                    "stablePlayerId": lobby_id,
                    "name": (
                        lobby_player.get("name")
                        or _decode_text(_text(state, "./Data/Name"))
                        or id_player.get("name")
                    ),
                    "number": (
                        lobby_player.get("number")
                        or _int(state, "./Data/Number")
                        or id_player.get("number")
                    ),
                }

                merge(state_id, {**identity, "playerId": state_id})
                if data_id is not None:
                    merge(data_id, {**identity, "playerId": data_id})

    # Preserve names even for PlayerData entries that never appear in a board
    # state. Team ownership may be unknown, but UI can still show the name.
    for player_id, identity in roster_by_id.items():
        merge(
            player_id,
            {
                "playerId": player_id,
                "stablePlayerId": identity.get("lobbyId"),
                **identity,
            },
        )

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
    if event_type in {"INJURY", "KO", "DEATH", "APOTHECARY"}:
        return _first_int(
            event,
            ("InjuredPlayerId", "KnockedOutPlayerId", "VictimId", "TargetId", "DefenderId", "PlayerId"),
        )
    return None


def enrich_match_event(
    item: dict[str, Any],
    raw_event: ET.Element,
    event_type: str,
    players: dict[int, dict[str, Any]],
    *,
    actor_player_id: int | None = None,
    affected_player_id: int | None = None,
) -> None:
    """Attach player identities and explicit actor/affected roles.

    CAS is actor-positive and belongs to the player/team that caused it.
    Injury/KO/death/apothecary belong to the affected player's timeline lane.
    """
    original_player_id = item.get("playerId")
    if actor_player_id is None and isinstance(original_player_id, int):
        actor_player_id = original_player_id
    if affected_player_id is None:
        affected_player_id = _target_player_id(raw_event, event_type)

    actor = players.get(actor_player_id) if actor_player_id is not None else None
    affected = players.get(affected_player_id) if affected_player_id is not None else None
    details = item.setdefault("details", {})

    actor_team = actor.get("teamId") if actor else None
    affected_team = affected.get("teamId") if affected else None
    actor_name = actor.get("name") if actor else None
    affected_name = affected.get("name") if affected else None

    if actor_player_id is not None:
        item["actorPlayerId"] = actor_player_id
        details["causingPlayerId"] = actor_player_id
    if actor_team is not None:
        item["causingTeamId"] = actor_team
        details["causingTeamId"] = actor_team
    if actor_name:
        item["actorPlayerName"] = actor_name
        details["causingPlayerName"] = actor_name

    if affected_player_id is not None:
        item["affectedPlayerId"] = affected_player_id
        details["affectedPlayerId"] = affected_player_id
        details["targetPlayerId"] = affected_player_id
    if affected_team is not None:
        item["affectedTeamId"] = affected_team
        details["affectedTeamId"] = affected_team
    if affected_name:
        item["affectedPlayerName"] = affected_name
        details["affectedPlayerName"] = affected_name
        details["targetPlayerName"] = affected_name
        details["injuredPlayerName"] = affected_name
    if affected and affected.get("number") is not None:
        details["targetPlayerNumber"] = affected["number"]

    affected_lane_types = {"INJURY", "KO", "DEATH", "APOTHECARY"}
    if event_type in affected_lane_types and affected:
        item["playerId"] = affected_player_id
        item["teamId"] = affected_team
        if affected_name:
            item["playerName"] = affected_name
        if affected.get("number") is not None:
            item["playerNumber"] = affected["number"]
    elif actor:
        item["playerId"] = actor_player_id
        if actor_team is not None:
            item["teamId"] = actor_team
        if actor_name:
            item["playerName"] = actor_name
        if actor.get("number") is not None:
            item["playerNumber"] = actor["number"]

    if event_type == "TOUCHDOWN" and actor_name:
        details["scorerName"] = actor_name
    elif event_type == "COMPLETION":
        if actor_name:
            details["throwerName"] = actor_name
        if affected_name:
            details["receiverName"] = affected_name
    elif event_type == "INTERCEPTION" and actor_name:
        details["interceptorName"] = actor_name
    elif event_type == "CASUALTY":
        if actor_name:
            details["causingPlayerName"] = actor_name
        if affected_name:
            details["injuredPlayerName"] = affected_name
    elif event_type in affected_lane_types:
        if affected_name:
            details["injuredPlayerName"] = affected_name
    elif actor_name:
        details["playerName"] = actor_name
