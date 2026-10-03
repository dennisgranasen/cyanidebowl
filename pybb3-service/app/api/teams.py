from dataclasses import fields, is_dataclass
from functools import lru_cache
import json
import threading
import time
import uuid
from typing import Any

from bb3.encoding import b64_decode_text
from bb3.models import Formation
from bb3.rules import BB3Rules
from fastapi import APIRouter,Depends,HTTPException,Query
from app.dependencies import trusted_owner
from app.schemas.team import (
    CharacteristicChoiceRequest,
    FormationDeleteRequest,
    FormationRequest,
    SkillAdvancementRequest,
)
from app.services.session_manager import SessionNotFound,session_manager
from app.services.team_mapper import teams_response
router=APIRouter(prefix="/sessions",tags=["Teams"])
_pending_rolls = {}
_pending_rolls_lock = threading.Lock()
_ROLL_TTL_SECONDS = 300


@lru_cache(maxsize=1)
def _bb3_rules() -> BB3Rules | None:
    return BB3Rules.from_env()


def _skill_name(skill_id: int) -> str | None:
    rules = _bb3_rules()
    if rules is None:
        return None
    try:
        return rules.skill_by_code(int(skill_id)).name
    except KeyError:
        return None


def _roster_with_skill_names(roster: Any) -> dict[str, Any]:
    result = _public_model(roster)
    for player in result.get("players", []):
        player["skill_names"] = {
            str(skill_id): name
            for skill_id in player.get("skill_ids", [])
            if (name := _skill_name(skill_id))
        }
    return result


def _improvements_with_skill_names(improvements: Any) -> dict[str, Any]:
    result = _public_model(improvements)
    for category in result.get("skill_categories", []):
        for skill in category.get("skills", []):
            skill["name"] = _skill_name(skill["skill_id"])
    return result


def _public_model(value: Any) -> Any:
    if is_dataclass(value):
        return {
            field.name: _public_model(getattr(value, field.name))
            for field in fields(value)
            if field.name != "raw_xml"
        }
    if isinstance(value, (list, tuple)):
        return [_public_model(item) for item in value]
    if isinstance(value, dict):
        return {key: _public_model(item) for key, item in value.items()}
    return value

def owned_team(client, team_id: str):
    start = 0
    while True:
        size = 100
        root = client.get_teams_of_gamer(size=size, start=start)
        result = teams_response(root, start=start, size=size)
        if any(str(team.get("id", "")).casefold() == team_id.casefold() for team in result["items"]):
            return client
        if not result["hasMore"]:
            break
        start += size
    raise HTTPException(404, "Team not found in this BB3 account")


def owned_player(client, team_id: str, player_id: str):
    roster = owned_team(client, team_id).get_team_roster_model(team_id)
    player = next((item for item in roster.players if item.player_id == player_id), None)
    if player is None:
        raise HTTPException(404, "Player not found in this BB3 team")
    return roster, player


def team_formations(client, team_id: str):
    root = owned_team(client, team_id).get_team_formations(team_id)
    result = []
    for item in root.findall(".//Formation"):
        encoded_data = item.findtext("Data") or ""
        try:
            data = json.loads(b64_decode_text(encoded_data)) if encoded_data else {}
        except (ValueError, json.JSONDecodeError) as error:
            raise ValueError("BB3 returned an invalid formation") from error
        pitch_map = data.get("pitchMap", {}) if isinstance(data, dict) else {}
        result.append({
            "formationId": _decode(item.findtext("Id")),
            "name": _decode(item.findtext("Name")) or "Formation",
            "formationType": _integer(item.findtext("Type")),
            "pitchMap": pitch_map if isinstance(pitch_map, dict) else {},
        })
    return result


def _decode(value):
    if not value:
        return None
    try:
        return b64_decode_text(value)
    except (ValueError, UnicodeDecodeError):
        return value


def _integer(value):
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def _prune_rolls(now=None):
    cutoff = (now or time.time()) - _ROLL_TTL_SECONDS
    for roll_id, record in list(_pending_rolls.items()):
        if record["createdAt"] < cutoff:
            _pending_rolls.pop(roll_id, None)

@router.get("/{session_id}/teams")
def my_teams(session_id:str,owner:str=Depends(trusted_owner),size:int=Query(50,ge=1,le=100),start:int=Query(0,ge=0)):
    try:
        root=session_manager.call(owner,session_id,lambda client:client.get_teams_of_gamer(size=size,start=start))
        result=teams_response(root,start=start,size=size)
        coach_id,coach_name=session_manager.coach_info(owner,session_id)
        for team in result["items"]:
            team["coachId"] = team.get("coachId") or coach_id
            team["coachName"] = team.get("coachName") or coach_name
        return result
    except SessionNotFound as error:raise HTTPException(404,str(error)) from error
    except (ValueError,RuntimeError,OSError) as error:raise HTTPException(502,"Unable to retrieve BB3 teams") from error
@router.get("/{session_id}/teams/{team_id}/roster")
def roster(session_id:str,team_id:str,owner:str=Depends(trusted_owner)):
    try:return session_manager.call(owner,session_id,lambda client:_roster_with_skill_names(owned_team(client, team_id).get_team_roster_model(team_id)))
    except SessionNotFound as error:raise HTTPException(404,str(error)) from error


@router.get("/{session_id}/teams/{team_id}/formations")
def formations(session_id: str, team_id: str, owner: str = Depends(trusted_owner)):
    try:
        return session_manager.call(owner, session_id, lambda client: team_formations(client, team_id))
    except SessionNotFound as error:
        raise HTTPException(404, str(error)) from error
    except (ValueError, RuntimeError, OSError) as error:
        raise HTTPException(502, "Unable to retrieve BB3 formations") from error


@router.post("/{session_id}/teams/{team_id}/formations")
def save_formation(
    session_id: str,
    team_id: str,
    request: FormationRequest,
    owner: str = Depends(trusted_owner),
):
    def save(client):
        owned_team(client, team_id)
        if not request.name.strip():
            raise HTTPException(400, "Formation name cannot be empty")
        if request.formation_id:
            known = {item["formationId"] for item in team_formations(client, team_id)}
            if request.formation_id not in known:
                raise HTTPException(404, "Formation not found in this BB3 team")
        if len(request.pitch_map) > 11:
            raise HTTPException(400, "A formation can place at most 11 players")
        formation = Formation(
            team_id=team_id,
            name=request.name.strip(),
            formation_type=request.formation_type,
            pitch_map=request.pitch_map,
            formation_id=request.formation_id,
        )
        return {"formationId": client.save_formation(formation)}

    try:
        return session_manager.call(owner, session_id, save)
    except SessionNotFound as error:
        raise HTTPException(404, str(error)) from error
    except (ValueError, RuntimeError, OSError) as error:
        raise HTTPException(502, "Unable to save BB3 formation") from error


@router.delete("/{session_id}/teams/{team_id}/formations")
def delete_formation(
    session_id: str,
    team_id: str,
    request: FormationDeleteRequest,
    owner: str = Depends(trusted_owner),
):
    def delete(client):
        known = {item["formationId"] for item in team_formations(client, team_id)}
        if request.formation_id not in known:
            raise HTTPException(404, "Formation not found in this BB3 team")
        client.remove_formations(team_id, [request.formation_id])
        return {"deleted": True}

    try:
        return session_manager.call(owner, session_id, delete)
    except SessionNotFound as error:
        raise HTTPException(404, str(error)) from error
    except (ValueError, RuntimeError, OSError) as error:
        raise HTTPException(502, "Unable to remove BB3 formation") from error


@router.get("/{session_id}/teams/{team_id}/players/{player_id}/improvements")
def player_improvements(
    session_id: str,
    team_id: str,
    player_id: str,
    owner: str = Depends(trusted_owner),
):
    def read(client):
        _, player = owned_player(client, team_id, player_id)
        return _improvements_with_skill_names(client.get_player_improvements(player.player_id))

    try:
        return session_manager.call(owner, session_id, read)
    except SessionNotFound as error:
        raise HTTPException(404, str(error)) from error
    except (ValueError, RuntimeError, OSError) as error:
        raise HTTPException(502, "Unable to retrieve BB3 player improvements") from error


@router.post("/{session_id}/teams/{team_id}/players/{player_id}/advancement/skill")
def advance_skill(
    session_id: str,
    team_id: str,
    player_id: str,
    request: SkillAdvancementRequest,
    owner: str = Depends(trusted_owner),
):
    def advance(client):
        _, player = owned_player(client, team_id, player_id)
        options = client.get_player_improvements(player.player_id)
        if request.kind == "random":
            category = next((item for item in options.skill_categories if item.category == request.category), None)
            if category is None or not category.random_available or not category.random_choosable:
                raise HTTPException(400, "Random skill advancement is not available")
            if player.spp < category.cost_random:
                raise HTTPException(400, "Player does not have enough SPP")
            result = client.add_player_random_skill(player.player_id, category.category)
            return {"kind": "random", "skillId": result.skill_id, "hasLeft": result.has_left}

        skill = next((
            skill for category in options.skill_categories for skill in category.skills
            if skill.skill_id == request.skill_id
        ), None)
        if skill is None or not skill.available or not skill.choosable:
            raise HTTPException(400, "Chosen skill advancement is not available")
        if player.spp < skill.cost:
            raise HTTPException(400, "Player does not have enough SPP")
        client.add_player_skill(player.player_id, skill.skill_id)
        return {"kind": "chosen", "skillId": skill.skill_id}

    try:
        return session_manager.call(owner, session_id, advance)
    except SessionNotFound as error:
        raise HTTPException(404, str(error)) from error
    except (ValueError, RuntimeError, OSError) as error:
        raise HTTPException(502, "Unable to advance BB3 player") from error


@router.post("/{session_id}/teams/{team_id}/players/{player_id}/advancement/characteristic/roll")
def roll_characteristic(
    session_id: str,
    team_id: str,
    player_id: str,
    owner: str = Depends(trusted_owner),
):
    def roll(client):
        _, player = owned_player(client, team_id, player_id)
        options = client.get_player_improvements(player.player_id)
        if not options.characteristic_available or not options.characteristic_choosable:
            raise HTTPException(400, "Characteristic advancement is not available")
        if player.spp < options.characteristic_cost:
            raise HTTPException(400, "Player does not have enough SPP")
        result = client.begin_increase_player_characteristic(player.player_id)
        roll_id = str(uuid.uuid4())
        with _pending_rolls_lock:
            _prune_rolls()
            _pending_rolls[roll_id] = {
                "owner": owner,
                "session": session_id,
                "team": team_id,
                "player": player.player_id,
                "createdAt": time.time(),
                "available": {item.characteristic_id for item in result.characteristics if item.available},
            }
        return {
            "rollId": roll_id,
            "roll": result.roll,
            "canTakeSecondarySkill": result.can_take_secondary_skill,
            "characteristics": _public_model(result.characteristics),
        }

    try:
        return session_manager.call(owner, session_id, roll)
    except SessionNotFound as error:
        raise HTTPException(404, str(error)) from error
    except (ValueError, RuntimeError, OSError) as error:
        raise HTTPException(502, "Unable to roll BB3 characteristic advancement") from error


@router.post("/{session_id}/teams/{team_id}/players/{player_id}/advancement/characteristic/choose")
def choose_characteristic(
    session_id: str,
    team_id: str,
    player_id: str,
    request: CharacteristicChoiceRequest,
    owner: str = Depends(trusted_owner),
):
    def choose(client):
        with _pending_rolls_lock:
            _prune_rolls()
            roll = _pending_rolls.get(request.roll_id)
            if not roll or (roll["owner"], roll["session"], roll["team"], roll["player"]) != (
                owner, session_id, team_id, player_id
            ):
                raise HTTPException(400, "Characteristic roll expired or does not match this player")
            if request.characteristic_id not in roll["available"]:
                raise HTTPException(400, "Characteristic was not available in this roll")
            _pending_rolls.pop(request.roll_id, None)
        _, player = owned_player(client, team_id, player_id)
        options = client.get_player_improvements(player.player_id)
        if player.spp < options.characteristic_cost:
            raise HTTPException(400, "Player does not have enough SPP")
        client.choose_increase_player_characteristic(player.player_id, request.characteristic_id)
        return {"characteristicId": request.characteristic_id}

    try:
        return session_manager.call(owner, session_id, choose)
    except SessionNotFound as error:
        raise HTTPException(404, str(error)) from error
    except (ValueError, RuntimeError, OSError) as error:
        raise HTTPException(502, "Unable to apply BB3 characteristic advancement") from error
