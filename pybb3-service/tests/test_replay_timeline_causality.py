import xml.etree.ElementTree as ET

from app.services.replay_timeline import (
    _action_actor,
    _actor_player,
    _find_source_action,
)


def test_block_before_damage_links_attacker_to_targeted_casualty():
    actions = [{
        "actionId": "bb3:144:0:block:5:43",
        "kind": "block",
        "attackerPlayerId": 5,
        "defenderPlayerId": 43,
        "outcome": "success",
    }]

    source, self_inflicted = _find_source_action(actions, 144, 1, 43)

    assert source is not None
    assert _action_actor(source) == 5
    assert source["actionId"] == "bb3:144:0:block:5:43"
    assert self_inflicted is False


def test_failed_dodge_before_damage_is_explicit_self_cause():
    actions = [{
        "actionId": "bb3:182:0:d6:42:-1:2:4",
        "kind": "d6",
        "actionType": "Dodge",
        "playerId": 42,
        "targetId": -1,
        "successful": False,
    }]

    source, self_inflicted = _find_source_action(actions, 182, 1, 42)

    assert source is not None
    assert _action_actor(source) == 42
    assert self_inflicted is True


def test_temporal_proximity_without_target_match_does_not_invent_causation():
    actions = [{
        "actionId": "bb3:108:1:d6:10:-1:2:3",
        "kind": "d6",
        "actionType": "Dodge",
        "playerId": 10,
        "targetId": -1,
        "successful": True,
    }]

    source, self_inflicted = _find_source_action(actions, 108, 2, 38)

    assert source is None
    assert self_inflicted is False


def test_damage_step_player_is_not_used_as_casualty_actor():
    event = ET.fromstring("<ResultCasualtyRoll />")

    actor = _actor_player(event, "CASUALTY", {"stepPlayerId": 43})

    assert actor is None
