import base64
import xml.etree.ElementTree as ET

from app.services.bb3_roll_types import (
    Bb3RollType,
    bb3_roll_category,
    bb3_roll_counts_as_action_stat,
    bb3_roll_name,
)
from app.services.replay_actions import D6Action
from app.services.replay_decoders import Bb3ActionDecoder
from app.services.replay_statistics import aggregate_actions


def _encoded(xml: str) -> str:
    return base64.b64encode(xml.encode()).decode()


def _replay_for_roll(roll_type: int, outcome: int = 1) -> ET.Element:
    step = "<Step><PlayerId>10</PlayerId><TargetId>20</TargetId></Step>"
    result = (
        f"<ResultRoll><RollType>{roll_type}</RollType><Difficulty>2</Difficulty>"
        f"<Outcome>{outcome}</Outcome><Dice><Die><DieType>0</DieType><Value>4</Value>"
        "</Die></Dice></ResultRoll>"
    )
    xml = f"""<Replay><ReplayStep><BoardState><ListTeams>
<TeamState><ListPitchPlayers><PlayerState><Id>10</Id><Status>0</Status><Situation>0</Situation></PlayerState></ListPitchPlayers></TeamState>
<TeamState><ListPitchPlayers><PlayerState><Id>20</Id><Status>0</Status><Situation>0</Situation></PlayerState></ListPitchPlayers></TeamState>
</ListTeams></BoardState><EventExecuteSequence><Sequence><StepResult>
<Step><MessageData>{_encoded(step)}</MessageData></Step><Results><StringMessage>
<MessageData>{_encoded(result)}</MessageData></StringMessage></Results>
</StepResult></Sequence></EventExecuteSequence></ReplayStep></Replay>"""
    return ET.fromstring(xml)


def test_new_roll_type_protocol_mapping():
    assert Bb3RollType.BallAndChainDirection.value == 87
    assert Bb3RollType.BombExplosionHit.value == 88
    assert Bb3RollType.Bloodlust.value == 96
    assert bb3_roll_name(87) == "Ball And Chain Direction"
    assert bb3_roll_name(88) == "Bomb Explosion Hit"
    assert bb3_roll_name(96) == "Bloodlust"
    assert bb3_roll_category(88) == "action"
    assert bb3_roll_category(96) == "action"


def test_bloodlust_and_bomb_explosion_are_action_d6_statistics():
    for roll_type, name in ((96, "Bloodlust"), (88, "Bomb Explosion Hit")):
        actions = Bb3ActionDecoder().decode(_replay_for_roll(roll_type))
        assert len(actions) == 1
        action = actions[0]
        assert isinstance(action, D6Action)
        assert action.action_type == name
        assert action.source_roll_type == roll_type
        assert action.roll_category == "action"

        stats = aggregate_actions(actions)
        assert stats == [{
            "eventType": name,
            "kind": "d6",
            "target": "2+",
            "teamId": 0,
            "success": 1,
            "total": 1,
            "sourceRollType": roll_type,
            "rollCategory": "action",
        }]


def test_ball_and_chain_direction_is_not_success_failure_action_stat():
    assert bb3_roll_category(87) == "system"
    assert bb3_roll_counts_as_action_stat(87) is False
    assert Bb3ActionDecoder().decode(_replay_for_roll(87)) == []


def test_unknown_roll_types_keep_previous_action_stat_behaviour():
    assert bb3_roll_counts_as_action_stat(999) is True
