from unittest.mock import Mock, patch

import pytest

from app.services.replay_timeline import ReplayTimelineError, build_replay_timeline


def test_build_replay_timeline_uses_pybb3_narrative_projection():
    narrative = {
        "format": "pybb3-narrative-timeline",
        "version": 1,
        "match": {"teams": [], "players": []},
        "events": [{
            "id": 1,
            "type": "move",
            "actor": {"kind": "player", "id": 41},
            "checks": [{
                "type": "dodge",
                "subject": {"kind": "player", "id": 41},
                "attempts": [
                    {"dice": [1], "outcome": "failed"},
                    {"dice": [3], "outcome": "passed", "reroll": "dodge"},
                ],
                "outcome": "passed",
                "reroll_used": True,
            }],
        }],
        "unresolved": {"FutureProtocolMessage": 1},
    }
    replay = Mock()
    timeline = Mock()
    replay.timeline.return_value = timeline
    timeline.to_narrative_dict.return_value = narrative

    with patch("app.services.replay_timeline.Replay.from_xml", return_value=replay) as from_xml:
        result = build_replay_timeline(b"<Replay/>")

    from_xml.assert_called_once_with(b"<Replay/>")
    replay.timeline.assert_called_once_with()
    timeline.to_narrative_dict.assert_called_once_with()
    assert result is narrative


def test_build_replay_timeline_rejects_unexpected_projection():
    replay = Mock()
    timeline = Mock()
    replay.timeline.return_value = timeline
    timeline.to_narrative_dict.return_value = {"format": "something-else"}

    with patch("app.services.replay_timeline.Replay.from_xml", return_value=replay):
        with pytest.raises(ReplayTimelineError, match="Unexpected pybb3"):
            build_replay_timeline(b"<Replay/>")


def _projected(value):
    replay = Mock()
    timeline = Mock()
    replay.timeline.return_value = timeline
    timeline.to_narrative_dict.return_value = value
    return replay


def test_build_replay_timeline_rejects_unsupported_contract_version():
    replay = _projected({
        "format": "pybb3-narrative-timeline",
        "version": 2,
        "match": {},
        "events": [],
        "unresolved": {},
    })

    with patch("app.services.replay_timeline.Replay.from_xml", return_value=replay):
        with pytest.raises(ReplayTimelineError, match="version"):
            build_replay_timeline(b"<Replay/>")


@pytest.mark.parametrize(
    ("field", "value", "message"),
    [
        ("match", [], "match must be an object"),
        ("events", {}, "events must be an array"),
        ("unresolved", [], "unresolved must be an object"),
    ],
)
def test_build_replay_timeline_rejects_malformed_contract_shape(field, value, message):
    narrative = {
        "format": "pybb3-narrative-timeline",
        "version": 1,
        "match": {},
        "events": [],
        "unresolved": {},
    }
    narrative[field] = value
    replay = _projected(narrative)

    with patch("app.services.replay_timeline.Replay.from_xml", return_value=replay):
        with pytest.raises(ReplayTimelineError, match=message):
            build_replay_timeline(b"<Replay/>")


def test_build_replay_timeline_wraps_pybb3_errors():
    with patch(
        "app.services.replay_timeline.Replay.from_xml",
        side_effect=ValueError("broken replay"),
    ):
        with pytest.raises(ReplayTimelineError, match="could not build"):
            build_replay_timeline(b"<Replay/>")
