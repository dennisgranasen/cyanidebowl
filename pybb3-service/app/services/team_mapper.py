from __future__ import annotations

import base64
import binascii
import xml.etree.ElementTree as ET


def teams_response(root: ET.Element, *, start: int, size: int) -> dict:
    """Map ResponseGetTeams to stable JSON without exposing protocol XML."""
    elements = root.findall("./Teams/Team") or root.findall(".//Team")
    owner_id = _decoded(root, "GamerId", "IdGamer") or _nested_decoded(root, ("Gamer",), ("Id", "GamerId"))
    owner_name = _decoded(root, "GamerName") or _nested_decoded(root, ("Gamer",), ("Name",))
    items = [_team(element, owner_id, owner_name) for element in elements]
    total = _first_int(root, "Total", "TotalCount", "NbTeams", "Count")
    return {
        "items": items, "start": start, "size": size, "count": len(items), "total": total,
        "hasMore": start + len(items) < total if total is not None else len(items) == size,
    }


def _team(team: ET.Element, owner_id: str | None = None, owner_name: str | None = None) -> dict:
    return {
        "id": _decoded(team, "Id", "IdTeam", "TeamId"),
        "name": _decoded(team, "Name"),
        "raceId": _first_int(team, "Race", "RaceId", "IdRace"),
        "teamValue": _first_int(team, "TeamValue", "Value"),
        "logoId": _decoded(team, "LogoId", "IdLogo"),
        "isCustom": _first_bool(team, "IsCustom"),
        "isTemplate": _first_bool(team, "IsTemplate"),
        "coachId": _decoded(team, "CoachId", "IdCoach", "GamerId", "IdGamer") or _nested_decoded(team, ("Coach", "Gamer"), ("Id", "IdCoach", "IdGamer")) or owner_id,
        "coachName": _decoded(team, "CoachName", "GamerName") or _nested_decoded(team, ("Coach", "Gamer"), ("Name",)) or owner_name,
    }


def _text(element: ET.Element, *names: str) -> str | None:
    for name in names:
        value = element.findtext(name)
        if value is not None and value != "": return value
    return None


def _decoded(element: ET.Element, *names: str) -> str | None:
    value = _text(element, *names)
    if value is None: return None
    try: return base64.b64decode(value, validate=True).decode("utf-8")
    except (binascii.Error, UnicodeDecodeError): return value


def _nested_decoded(element: ET.Element, parents: tuple[str, ...], names: tuple[str, ...]) -> str | None:
    for parent_name in parents:
        parent = element.find(parent_name)
        if parent is not None:
            value = _decoded(parent, *names)
            if value is not None: return value
    return None


def _first_int(element: ET.Element, *names: str) -> int | None:
    value = _text(element, *names)
    try: return int(value) if value is not None else None
    except ValueError: return None


def _first_bool(element: ET.Element, *names: str) -> bool | None:
    value = _text(element, *names)
    return None if value is None else value.lower() == "true"
