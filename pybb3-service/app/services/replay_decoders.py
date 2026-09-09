from __future__ import annotations

import base64
import io
import zipfile
import xml.etree.ElementTree as ET
from collections import defaultdict
from typing import Any

from .bb3_roll_types import bb3_roll_category, bb3_roll_name
from .replay_actions import (
    BLOCK_FACE_NAMES,
    BloodlustAction,
    BlockAction,
    BlockChooser,
    BlockOutcome,
    BombAction,
    ChainsawAction,
    D6Action,
    FoulAction,
    HypnoticGazeAction,
    KickTeamMateAction,
    ProjectileVomitAction,
    SpecialAction,
    SpecialOutcome,
    StabAction,
    ThrowTeamMateAction,
    WizardAction,
)


SPECIAL_ACTION_TYPES: tuple[tuple[tuple[str, ...], type[SpecialAction]], ...] = (
    (("THROWTEAMMATE", "THROWTEAMMATEACTION", "TTMACTION"), ThrowTeamMateAction),
    (("KICKTEAMMATE", "KICKTEAMMATEACTION"), KickTeamMateAction),
    (("HYPNOTICGAZE", "HYPNOTICGAZEACTION"), HypnoticGazeAction),
    (("PROJECTILEVOMIT", "PROJECTILEVOMITACTION"), ProjectileVomitAction),
    (("BLOODLUST", "BLOODLUSTACTION"), BloodlustAction),
    (("CHAINSAW", "CHAINSAWACTION"), ChainsawAction),
    (("STAB", "STABACTION"), StabAction),
    (("FOUL", "FOULACTION"), FoulAction),
    (("THROWBOMB", "BOMBTHROW", "BOMBACTION", "BOMB"), BombAction),
    (("WIZARD", "FIREBALL", "ZAP", "THUNDERBOLT"), WizardAction),
)


def _token(value: str | None) -> str:
    return "".join(character for character in (value or "").upper() if character.isalnum())


def _special_class(nodes: list[ET.Element]) -> type[SpecialAction] | None:
    """Recognise explicit special-action markers without guessing from dice rolls."""
    tokens: set[str] = set()
    identity_fields = (
        "ActionType", "ActionName", "SpecialAction", "SkillName", "SpellType",
        "EventType", "Name", "Type",
    )
    for node in nodes:
        tokens.add(_token(node.tag))
        for field_name in identity_fields:
            value = node.findtext(field_name)
            if value:
                tokens.add(_token(value))
    for aliases, action_class in SPECIAL_ACTION_TYPES:
        if any(token == alias or token.endswith(alias) or token.startswith(alias) for token in tokens for alias in aliases):
            return action_class
    return None


def _special_outcome(nodes: list[ET.Element]) -> SpecialOutcome:
    for node in reversed(nodes):
        for field_name in ("Successful", "Success", "Outcome", "ActionOutcome"):
            value = node.findtext(field_name)
            if value is None:
                continue
            token = _token(value)
            if token in {"1", "TRUE", "SUCCESS", "SUCCEEDED"}:
                return SpecialOutcome.SUCCESS
            if token in {"0", "FALSE", "FAIL", "FAILED", "FAILURE"}:
                return SpecialOutcome.FAILURE
            if token in {"NEUTRAL", "PUSH"}:
                return SpecialOutcome.NEUTRAL
    return SpecialOutcome.UNKNOWN


def _special_rolls(nodes: list[ET.Element]) -> list[int]:
    rolls: list[int] = []
    for node in nodes:
        for die in node.findall(".//Dice/Die"):
            value = number(die, "Value")
            if value is not None:
                rolls.append(value)
    return rolls


def _raw_event(node: ET.Element) -> dict[str, Any]:
    return {"tag": node.tag, "xml": ET.tostring(node, encoding="unicode")}


def _first_number(nodes: list[ET.Element], *field_names: str) -> int | None:
    for node in nodes:
        for field_name in field_names:
            value = number(node, field_name)
            if value is not None:
                return value
    return None


def _special_details(nodes: list[ET.Element]) -> dict[str, Any]:
    details: dict[str, Any] = {"messageTags": [node.tag for node in nodes]}
    for key in ("SpellType", "SkillName", "SkillId", "ActionType", "ActionName"):
        for node in nodes:
            value = node.findtext(key)
            if value not in (None, ""):
                details[key[0].lower() + key[1:]] = value
                break
    return details


def number(node: ET.Element | None, path: str, default=None):
    if node is None:
        return default
    value = node.findtext(path)
    try:
        return int(value) if value is not None else default
    except ValueError:
        return default


def decode_message(node: ET.Element | None) -> ET.Element | None:
    if node is None:
        return None
    payload = node.findtext("MessageData")
    if not payload:
        return None
    try:
        raw = payload.encode()
        for _ in range(2):
            raw = base64.b64decode(raw, validate=True)
            if raw.lstrip().startswith(b"<"):
                if b"<!DOCTYPE" in raw.upper():
                    return None
                return ET.fromstring(raw)
    except (ValueError, ET.ParseError):
        return None
    return None


def board_players(board: ET.Element | None) -> dict[int, tuple[int, ET.Element]]:
    players: dict[int, tuple[int, ET.Element]] = {}
    if board is not None:
        for team, state in enumerate(board.findall("ListTeams/TeamState")):
            for player in state.findall("ListPitchPlayers/PlayerState"):
                player_id = number(player, "Id")
                if player_id is not None:
                    players[player_id] = (team, player)
    return players


def _down(player: ET.Element) -> bool:
    return number(player, "Status", 0) in (1, 2) or number(player, "Situation", 0) != 0


def resolve_block_outcome(
    outcome: ET.Element,
    players: dict[int, tuple[int, ET.Element]],
) -> tuple[BlockOutcome, bool | None, bool | None, bool]:
    attacker = players.get(number(outcome, "AttackerId"))
    defender = players.get(number(outcome, "DefenderId"))
    attacker_down = _down(attacker[1]) if attacker else None
    defender_down = _down(defender[1]) if defender else None

    defender_surfed = False
    for push in outcome.findall("Pushbacks/ResultPushBack"):
        x, y = number(push, "CellTo/X", 0), number(push, "CellTo/Y", 0)
        if not (0 <= x < 26 and 0 <= y < 15):
            defender_surfed = True
            break

    # A surf is a positive block result even when the defender never becomes
    # prone in the board state. Otherwise use the resolved board consequence,
    # not the selected die face: skills can alter the face's normal effect.
    if defender_surfed:
        resolved = BlockOutcome.SUCCESS
    elif attacker_down is True:
        resolved = BlockOutcome.NEUTRAL if defender_down is True else BlockOutcome.FAILURE
    elif defender_down is True:
        resolved = BlockOutcome.SUCCESS
    elif attacker_down is None or defender_down is None:
        resolved = BlockOutcome.UNKNOWN
    else:
        resolved = BlockOutcome.NEUTRAL
    return resolved, attacker_down, defender_down, defender_surfed


class Bb3ActionDecoder:
    """Decode BB3 sequence messages into canonical replay actions."""

    source = "BB3"

    def decode(self, root: ET.Element) -> list[D6Action | BlockAction | SpecialAction]:
        actions: list[D6Action | BlockAction | SpecialAction] = []
        owners: dict[int, int] = {}
        pending_blocks: dict[tuple[Any, Any], dict[str, Any]] = {}

        for replay_index, replay_step in enumerate(root.findall("ReplayStep")):
            players = board_players(replay_step.find("BoardState"))
            owners.update({pid: value[0] for pid, value in players.items()})

            for sequence_index, step_result in enumerate(
                replay_step.findall(".//EventExecuteSequence/Sequence/StepResult")
            ):
                step = decode_message(step_result.find("Step"))
                if step is None or number(step, "IsEvaluation", 0):
                    continue
                player_id = number(step, "PlayerId")
                target_id = number(step, "TargetId")
                team_id = owners.get(player_id)
                block_key = (player_id, target_id)
                roll_groups: dict[tuple[int, int], list[ET.Element]] = defaultdict(list)
                decoded_results = [
                    result
                    for wrapper in step_result.findall("Results/StringMessage")
                    if (result := decode_message(wrapper)) is not None
                ]
                special_nodes = [step, *decoded_results]
                special_class = _special_class(special_nodes)

                for result in decoded_results:
                    if result.tag == "ResultRoll":
                        roll_type = number(result, "RollType")
                        difficulty = number(result, "Difficulty")
                        if roll_type is not None and difficulty is not None:
                            roll_groups[(roll_type, difficulty)].append(result)
                    elif result.tag == "QuestionBlockDice":
                        dice = result.findall("Dice/Die")
                        pending_blocks[block_key] = {
                            "diceCount": len(dice) or None,
                            "chooser": BlockChooser.DEFENDER
                            if number(result, "AttackerChoice", 1) == 0
                            else BlockChooser.ATTACKER,
                            "rolledFaces": [
                                BLOCK_FACE_NAMES.get(number(die, "Value"), "unknown") for die in dice
                            ],
                        }
                    elif result.tag == "ResultBlockRoll":
                        pending_blocks.setdefault(block_key, {})["selectedFace"] = BLOCK_FACE_NAMES.get(
                            number(result, "Die/Value"), "unknown"
                        )
                    elif result.tag == "ResultBlockOutcome":
                        attacker_id = number(result, "AttackerId")
                        defender_id = number(result, "DefenderId")
                        actual_key = (attacker_id, defender_id)
                        pending = pending_blocks.pop(actual_key, None)
                        if pending is None:
                            pending = pending_blocks.pop(block_key, {})
                        resolved, attacker_down, defender_down, surfed = resolve_block_outcome(result, players)
                        actions.append(
                            BlockAction(
                                action_id=f"bb3:{replay_index}:{sequence_index}:block:{attacker_id}:{defender_id}",
                                attacker_team_id=owners.get(attacker_id, team_id),
                                attacker_player_id=attacker_id,
                                defender_player_id=defender_id,
                                dice_count=pending.get("diceCount"),
                                chooser=pending.get("chooser", BlockChooser.ATTACKER),
                                rolled_faces=pending.get("rolledFaces", []),
                                selected_face=pending.get("selectedFace", "unknown"),
                                outcome=resolved,
                                attacker_down=attacker_down,
                                defender_down=defender_down,
                                defender_surfed=surfed,
                            )
                        )

                if special_class is not None:
                    # A special action owns the rolls in this StepResult. Keep
                    # them on the special action so Stab/Chainsaw/Wizard/etc. do
                    # not inflate normal Dodge/Pass/other D6 statistics.
                    acting_player_id = player_id if player_id is not None else _first_number(
                        special_nodes, "PlayerId", "AttackerId", "ThrowerId", "CasterId"
                    )
                    special_target_id = target_id if target_id is not None else _first_number(
                        special_nodes, "TargetId", "DefenderId", "VictimId"
                    )
                    acting_team_id = owners.get(acting_player_id, team_id)
                    if acting_team_id is None:
                        acting_team_id = _first_number(
                            special_nodes, "TeamId", "AttackerTeamId", "ActingTeamId", "CasterTeamId"
                        )
                    target_team_id = owners.get(special_target_id)
                    if target_team_id is None:
                        target_team_id = _first_number(special_nodes, "TargetTeamId", "DefenderTeamId")
                    actions.append(
                        special_class(
                            action_id=(
                                f"bb3:{replay_index}:{sequence_index}:special:"
                                f"{special_class.__name__}:{acting_player_id}:{special_target_id}"
                            ),
                            team_id=acting_team_id,
                            player_id=acting_player_id,
                            target_team_id=target_team_id,
                            target_player_id=special_target_id,
                            rolls=_special_rolls(special_nodes),
                            outcome=_special_outcome(special_nodes),
                            details=_special_details(special_nodes),
                            raw_events=[_raw_event(node) for node in special_nodes],
                        )
                    )
                    continue

                # All rolls with the same roll type+difficulty inside one
                # StepResult are one attempted action. This makes a failed roll
                # followed by a reroll and success count as 1/1, while keeping
                # both raw die values for luck analysis.
                for (roll_type, difficulty), results in roll_groups.items():
                    dice_values: list[int] = []
                    final_outcome = None
                    for result in results:
                        dice = result.findall("Dice/Die")
                        if len(dice) != 1 or number(dice[0], "DieType") != 0:
                            continue
                        value = number(dice[0], "Value")
                        if value is not None:
                            dice_values.append(value)
                        outcome = number(result, "Outcome")
                        if outcome in (0, 1):
                            final_outcome = bool(outcome)
                    if team_id is None or not dice_values or final_outcome is None or difficulty < 1:
                        continue
                    actions.append(
                        D6Action(
                            action_id=(
                                f"bb3:{replay_index}:{sequence_index}:d6:"
                                f"{player_id}:{target_id}:{roll_type}:{difficulty}"
                            ),
                            action_type=bb3_roll_name(roll_type),
                            team_id=team_id,
                            player_id=player_id,
                            target_id=target_id,
                            target=difficulty,
                            rolls=dice_values,
                            successful=final_outcome,
                            reroll_used=len(dice_values) > 1,
                            source_roll_type=roll_type,
                            roll_category=bb3_roll_category(roll_type),
                        )
                    )
        return actions


class Bb2ReplayDecoder:
    """BB2 .bbrz container decoder.

    A BB2 replay is a ZIP archive containing XML. Extraction is deliberately
    separated from BB3 action decoding so BB2 event mappings can grow without
    creating a second statistics implementation.
    """

    source = "BB2"

    @staticmethod
    def extract_xml(data: bytes) -> bytes:
        try:
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                files = [name for name in archive.namelist() if not name.endswith("/")]
                if not files:
                    raise ValueError("BB2 replay archive is empty")
                # bbrz-parser uses the first file in the archive. Prefer XML if
                # there are auxiliary files, but retain that compatible fallback.
                xml_name = next((name for name in files if name.lower().endswith(".xml")), files[0])
                xml = archive.read(xml_name)
        except (zipfile.BadZipFile, KeyError) as error:
            raise ValueError("Invalid BB2 .bbrz replay archive") from error
        if b"<!DOCTYPE" in xml.upper():
            raise ValueError("Replay XML must not contain a document type declaration")
        try:
            ET.fromstring(xml)
        except ET.ParseError as error:
            raise ValueError("BB2 replay archive did not contain valid XML") from error
        return xml

    def decode(self, root: ET.Element) -> list[D6Action | BlockAction | SpecialAction]:
        # BB2 XML layouts differ from BB3's encoded sequence messages. Keep the
        # canonical boundary here: unsupported events remain available in the
        # compact/raw replay and do not get guessed into statistics.
        return []


def action_dicts(actions: list[D6Action | BlockAction | SpecialAction]) -> list[dict[str, Any]]:
    return [action.to_dict() for action in actions]
