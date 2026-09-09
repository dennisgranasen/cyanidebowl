import base64
import io
import zipfile
import xml.etree.ElementTree as ET

from app.services.bb3_roll_types import Bb3RollType, bb3_roll_category, bb3_roll_name
from app.services.replay_actions import BlockAction, BlockChooser, BlockOutcome, D6Action
from app.services.replay_decoders import Bb2ReplayDecoder, Bb3ActionDecoder, resolve_block_outcome
from app.services.replay_statistics import aggregate_actions


def encoded(xml: str) -> str:
    return base64.b64encode(xml.encode()).decode()


def string_message(xml: str) -> str:
    return f"<StringMessage><MessageData>{encoded(xml)}</MessageData></StringMessage>"


def step_result(step_xml: str, results: list[str]) -> str:
    return (
        "<StepResult><Step><MessageData>"
        + encoded(step_xml)
        + "</MessageData></Step><Results>"
        + "".join(string_message(result) for result in results)
        + "</Results></StepResult>"
    )


def board(attacker_status=0, defender_status=0) -> str:
    return f"""<BoardState><ListTeams>
<TeamState><ListPitchPlayers><PlayerState><Id>10</Id><Status>{attacker_status}</Status><Situation>0</Situation></PlayerState></ListPitchPlayers></TeamState>
<TeamState><ListPitchPlayers><PlayerState><Id>20</Id><Status>{defender_status}</Status><Situation>0</Situation></PlayerState></ListPitchPlayers></TeamState>
</ListTeams></BoardState>"""


def replay_with_sequence(sequence: str, board_xml: str | None = None) -> ET.Element:
    return ET.fromstring(
        "<Replay><ReplayStep>"
        + (board_xml or board())
        + "<EventExecuteSequence><Sequence>"
        + sequence
        + "</Sequence></EventExecuteSequence></ReplayStep></Replay>"
    )


def test_d6_reroll_is_one_action_with_all_raw_rolls():
    result = step_result(
        "<Step><PlayerId>10</PlayerId><TargetId>99</TargetId></Step>",
        [
            "<ResultRoll><RollType>2</RollType><Difficulty>3</Difficulty><Outcome>0</Outcome><Dice><Die><DieType>0</DieType><Value>2</Value></Die></Dice></ResultRoll>",
            "<ResultRoll><RollType>2</RollType><Difficulty>3</Difficulty><Outcome>1</Outcome><Dice><Die><DieType>0</DieType><Value>5</Value></Die></Dice></ResultRoll>",
        ],
    )
    actions = Bb3ActionDecoder().decode(replay_with_sequence(result))
    assert len(actions) == 1
    dodge = actions[0]
    assert isinstance(dodge, D6Action)
    assert dodge.action_type == "Dodge"
    assert dodge.target == 3
    assert dodge.rolls == [2, 5]
    assert dodge.successful is True
    assert dodge.reroll_used is True

    stats = aggregate_actions(actions)
    assert stats == [{
        "eventType": "Dodge", "kind": "d6", "target": "3+", "teamId": 0,
        "success": 1, "total": 1, "sourceRollType": 2, "rollCategory": "action",
    }]


def test_full_bb3_roll_type_reference_contains_known_problem_values():
    assert Bb3RollType.AnimalSavagery.value == 36
    assert Bb3RollType.FoulAppearance.value == 37
    assert Bb3RollType.Regeneration.value == 46
    assert Bb3RollType.HypnoticGaze.value == 66
    assert bb3_roll_name(36) == "Animal Savagery"
    assert bb3_roll_name(37) == "Foul Appearance"
    assert bb3_roll_name(46) == "Regeneration"
    assert bb3_roll_name(66) == "Hypnotic Gaze"
    assert bb3_roll_category(36) == "skillTrait"
    assert bb3_roll_category(46) == "injuryRecovery"


def test_bb3_decoder_uses_protocol_enum_and_preserves_source_roll_type():
    cases = [
        (36, "Animal Savagery", "skillTrait"),
        (37, "Foul Appearance", "skillTrait"),
        (46, "Regeneration", "injuryRecovery"),
    ]
    for roll_type, expected_name, expected_category in cases:
        result = step_result(
            "<Step><PlayerId>10</PlayerId><TargetId>20</TargetId></Step>",
            [
                f"<ResultRoll><RollType>{roll_type}</RollType><Difficulty>2</Difficulty>"
                "<Outcome>1</Outcome><Dice><Die><DieType>0</DieType><Value>4</Value>"
                "</Die></Dice></ResultRoll>"
            ],
        )
        actions = Bb3ActionDecoder().decode(replay_with_sequence(result))
        assert len(actions) == 1
        action = actions[0]
        assert isinstance(action, D6Action)
        assert action.action_type == expected_name
        assert action.source_roll_type == roll_type
        assert action.roll_category == expected_category


def test_unknown_bb3_roll_type_is_not_guessed():
    assert bb3_roll_name(999) == "BB3 RollType 999"
    assert bb3_roll_category(999) == "unknown"


def test_minus_two_d_block_keeps_all_offered_faces_and_selected_face():
    result = step_result(
        "<Step><PlayerId>10</PlayerId><TargetId>20</TargetId></Step>",
        [
            "<QuestionBlockDice><AttackerChoice>0</AttackerChoice><Dice>"
            "<Die><Value>0</Value></Die><Die><Value>2</Value></Die></Dice></QuestionBlockDice>",
            "<ResultBlockRoll><Die><Value>0</Value></Die></ResultBlockRoll>",
            "<ResultBlockOutcome><AttackerId>10</AttackerId><DefenderId>20</DefenderId></ResultBlockOutcome>",
        ],
    )
    actions = Bb3ActionDecoder().decode(replay_with_sequence(result, board(attacker_status=1)))
    block = actions[0]
    assert isinstance(block, BlockAction)
    assert block.dice_count == 2
    assert block.chooser is BlockChooser.DEFENDER
    assert block.dice_label == "-2D"
    assert block.rolled_faces == ["skull", "push"]
    assert block.selected_face == "skull"
    assert block.outcome is BlockOutcome.FAILURE


def test_block_outcome_regular_push_is_neutral():
    outcome = ET.fromstring("<ResultBlockOutcome><AttackerId>10</AttackerId><DefenderId>20</DefenderId></ResultBlockOutcome>")
    players = {10: (0, ET.fromstring("<PlayerState><Status>0</Status><Situation>0</Situation></PlayerState>")),
               20: (1, ET.fromstring("<PlayerState><Status>0</Status><Situation>0</Situation></PlayerState>"))}
    resolved, _, _, surfed = resolve_block_outcome(outcome, players)
    assert resolved is BlockOutcome.NEUTRAL
    assert surfed is False


def test_block_outcome_push_to_crowd_is_success():
    outcome = ET.fromstring("""<ResultBlockOutcome><AttackerId>10</AttackerId><DefenderId>20</DefenderId>
<Pushbacks><ResultPushBack><CellTo><X>-1</X><Y>7</Y></CellTo></ResultPushBack></Pushbacks></ResultBlockOutcome>""")
    players = {10: (0, ET.fromstring("<PlayerState><Status>0</Status><Situation>0</Situation></PlayerState>")),
               20: (1, ET.fromstring("<PlayerState><Status>0</Status><Situation>0</Situation></PlayerState>"))}
    resolved, _, _, surfed = resolve_block_outcome(outcome, players)
    assert resolved is BlockOutcome.SUCCESS
    assert surfed is True


def test_block_outcome_both_down_is_neutral():
    outcome = ET.fromstring("<ResultBlockOutcome><AttackerId>10</AttackerId><DefenderId>20</DefenderId></ResultBlockOutcome>")
    players = {10: (0, ET.fromstring("<PlayerState><Status>1</Status><Situation>0</Situation></PlayerState>")),
               20: (1, ET.fromstring("<PlayerState><Status>1</Status><Situation>0</Situation></PlayerState>"))}
    resolved, _, _, _ = resolve_block_outcome(outcome, players)
    assert resolved is BlockOutcome.NEUTRAL


def test_block_outcome_attacker_only_down_is_failure():
    outcome = ET.fromstring("<ResultBlockOutcome><AttackerId>10</AttackerId><DefenderId>20</DefenderId></ResultBlockOutcome>")
    players = {10: (0, ET.fromstring("<PlayerState><Status>1</Status><Situation>0</Situation></PlayerState>")),
               20: (1, ET.fromstring("<PlayerState><Status>0</Status><Situation>0</Situation></PlayerState>"))}
    resolved, _, _, _ = resolve_block_outcome(outcome, players)
    assert resolved is BlockOutcome.FAILURE


def test_bb2_bbrz_extracts_xml_without_external_dependency():
    source = b"<Replay><ReplayStep><GameInfos><Id>bb2-test</Id></GameInfos></ReplayStep></Replay>"
    archive = io.BytesIO()
    with zipfile.ZipFile(archive, "w") as handle:
        handle.writestr("replay.xml", source)
    assert Bb2ReplayDecoder.extract_xml(archive.getvalue()) == source


def test_stab_is_explicit_special_action_and_does_not_pollute_d6_stats():
    result = step_result(
        "<Step><ActionType>Stab</ActionType><PlayerId>10</PlayerId><TargetId>20</TargetId></Step>",
        [
            "<ResultRoll><RollType>2</RollType><Difficulty>3</Difficulty><Outcome>1</Outcome>"
            "<Dice><Die><DieType>0</DieType><Value>5</Value></Die></Dice></ResultRoll>",
        ],
    )
    actions = Bb3ActionDecoder().decode(replay_with_sequence(result))
    assert len(actions) == 1
    stab = actions[0]
    from app.services.replay_actions import StabAction, SpecialOutcome
    assert isinstance(stab, StabAction)
    assert stab.rolls == [5]
    assert stab.outcome is SpecialOutcome.SUCCESS
    assert stab.raw_events[0]["tag"] == "Step"

    stats = aggregate_actions(actions)
    assert stats == [{
        "eventType": "Stab", "kind": "special", "target": "All", "teamId": 0,
        "success": 1, "neutral": 0, "fail": 0, "unknown": 0, "total": 1,
    }]
    assert not any(row["kind"] == "d6" for row in stats)


def test_wizard_spell_is_canonical_wizard_action_with_spell_type_preserved():
    result = step_result(
        "<Step><ActionType>Wizard</ActionType><SpellType>Fireball</SpellType><PlayerId>10</PlayerId><TargetId>20</TargetId></Step>",
        ["<ResultWizard><Success>true</Success></ResultWizard>"],
    )
    actions = Bb3ActionDecoder().decode(replay_with_sequence(result))
    from app.services.replay_actions import WizardAction, SpecialOutcome
    assert len(actions) == 1
    wizard = actions[0]
    assert isinstance(wizard, WizardAction)
    assert wizard.details["spellType"] == "Fireball"
    assert wizard.outcome is SpecialOutcome.SUCCESS
    assert wizard.target_player_id == 20


def test_supported_special_action_markers_have_distinct_canonical_types():
    from app.services.replay_actions import (
        BloodlustAction, BombAction, ChainsawAction, FoulAction,
        HypnoticGazeAction, KickTeamMateAction, ProjectileVomitAction,
        StabAction, ThrowTeamMateAction, WizardAction,
    )

    cases = {
        "Stab": StabAction,
        "Wizard": WizardAction,
        "ThrowBomb": BombAction,
        "Chainsaw": ChainsawAction,
        "Foul": FoulAction,
        "ThrowTeamMate": ThrowTeamMateAction,
        "KickTeamMate": KickTeamMateAction,
        "HypnoticGaze": HypnoticGazeAction,
        "ProjectileVomit": ProjectileVomitAction,
        "Bloodlust": BloodlustAction,
    }
    for action_type, expected in cases.items():
        result = step_result(
            f"<Step><ActionType>{action_type}</ActionType><PlayerId>10</PlayerId><TargetId>20</TargetId></Step>",
            [],
        )
        actions = Bb3ActionDecoder().decode(replay_with_sequence(result))
        assert len(actions) == 1, action_type
        assert isinstance(actions[0], expected), action_type


def test_unrecognised_action_is_not_guessed_into_special_statistics():
    result = step_result(
        "<Step><ActionType>FutureMysteryMechanic</ActionType><PlayerId>10</PlayerId><TargetId>20</TargetId></Step>",
        [],
    )
    actions = Bb3ActionDecoder().decode(replay_with_sequence(result))
    assert actions == []
