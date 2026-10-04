import base64
import json
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from types import SimpleNamespace

import pytest
from fastapi import HTTPException
from bb3.rules import BB3Rules

from app.api import teams as team_api
from app.schemas.team import SkillAdvancementRequest


def encoded(value):
    return base64.b64encode(value.encode()).decode()


class FakeClient:
    def __init__(self, team_pages=None, players=()):
        self.team_pages = team_pages or {}
        self.players = players
        self.skill_mutations = []
        self.roster_reads = []

    def get_teams_of_gamer(self, *, size, start):
        page = self.team_pages.get(start, ([], 0))
        team_items, total = page
        items = "".join(f"<Team><Id>{encoded(team_id)}</Id></Team>" for team_id in team_items)
        return ET.fromstring(f"<ResponseGetTeams><Total>{total}</Total><Teams>{items}</Teams></ResponseGetTeams>")

    def get_team_roster_model(self, team_id):
        self.roster_reads.append(team_id)
        return SimpleNamespace(players=self.players)

    def get_team(self, _team_id):
        return ET.fromstring(
            "<ResponseGetTeam><Team><Name>Live team</Name><Treasury>120000</Treasury>"
            "<TeamValue>1350000</TeamValue><AssistantCoaches>2</AssistantCoaches></Team></ResponseGetTeam>"
        )

    def get_player_improvements(self, _player_id):
        skill = SimpleNamespace(skill_id=7, available=False, choosable=False, cost=4)
        category = SimpleNamespace(category=1, skills=(skill,), random_available=False,
                                   random_choosable=False, cost_random=4)
        return SimpleNamespace(skill_categories=(category,))

    def add_player_skill(self, player_id, skill_id):
        self.skill_mutations.append((player_id, skill_id))


def test_owned_team_follows_pages_until_team_is_found():
    client = FakeClient({0: (["first"], 101), 100: (["wanted"], 101)})

    assert team_api.owned_team(client, "wanted") is client


def test_owned_team_rejects_a_team_outside_account():
    client = FakeClient({0: (["someone-elses-team"], 1)})

    with pytest.raises(HTTPException) as error:
        team_api.owned_team(client, "wanted")

    assert error.value.status_code == 404


def test_live_roster_reads_team_without_ownership_check(monkeypatch):
    client = FakeClient({0: (["someone-elses-team"], 1)}, players=[])
    monkeypatch.setattr(team_api.session_manager, "call", lambda _owner, _session, operation: operation(client))
    monkeypatch.setattr(team_api, "_roster_with_skill_names", lambda _roster: {"players": []})

    result = team_api.live_roster("session-1", "public-team", owner="owner-1")

    assert result["players"] == []
    assert result["team"]["cash"] == 120000
    assert result["team"]["coachAssistants"] == 2
    assert client.roster_reads == ["public-team"]


def test_formations_response_uses_items_object(monkeypatch):
    client = FakeClient()
    monkeypatch.setattr(team_api.session_manager, "call", lambda _owner, _session, operation: operation(client))
    monkeypatch.setattr(team_api, "team_formations", lambda _client, _team_id: [])

    result = team_api.formations("session-1", "team-1", owner="owner-1")

    assert result == {"items": []}


def test_roster_characteristics_include_frontend_attribute_values():
    roster = {"players": [{
        "characteristics": [
            {"characteristic_id": 0, "value": 6, "bonuses": 0, "maluses": 0},
            {"characteristic_id": 1, "value": 3, "bonuses": 0, "maluses": 0},
            {"characteristic_id": 2, "value": 3, "bonuses": 0, "maluses": 0},
            {"characteristic_id": 3, "value": 4, "bonuses": 0, "maluses": 0},
            {"characteristic_id": 4, "value": 9, "bonuses": 0, "maluses": 0},
        ],
        "skill_ids": [],
    }]}

    result = team_api._roster_with_skill_names(roster)

    assert result["players"][0]["attributes"] == {"ma": 6, "st": 3, "ag": 3, "pa": 4, "av": 9}


def test_roster_fills_position_type_and_default_characteristics(tmp_path, monkeypatch):
    rules_path = tmp_path / "BB3Rules.json"
    rules_path.write_text(json.dumps({
        "bb3_rules_position": [{"code": 55, "data": "Thrall"}],
    }))
    monkeypatch.setattr(team_api, "_bb3_rules", lambda: BB3Rules.load(rules_path))
    roster = {
        "positions": [{
            "position_id": 55,
            "characteristics": [
                {"characteristic_id": 0, "value": 6},
                {"characteristic_id": 1, "value": 3},
                {"characteristic_id": 2, "value": 3},
                {"characteristic_id": 3, "value": 4},
                {"characteristic_id": 4, "value": 8},
            ],
        }],
        "players": [{"position_id": 55, "characteristics": [], "skill_ids": []}],
    }

    player = team_api._roster_with_skill_names(roster)["players"][0]

    assert player["type"] == "Thrall"
    assert player["attributes"] == {"ma": 6, "st": 3, "ag": 3, "pa": 4, "av": 8}


def test_empty_roster_diagnostic_logs_xml_paths_without_player_data(caplog):
    @dataclass
    class Roster:
        players: tuple
        raw_xml: str

    roster = Roster(
        players=(),
        raw_xml=(
            "<ResponseGetTeamRoster><Roster><TeamRoster><Slot><Player>"
            "<Name>private-player-name</Name></Player></Slot></TeamRoster></Roster>"
            "</ResponseGetTeamRoster>"
        ),
    )

    with caplog.at_level("WARNING"):
        team_api._roster_with_skill_names(roster)

    assert "ResponseGetTeamRoster/Roster/TeamRoster/Slot/Player" in caplog.text
    assert "1 Player and 0 TeamRosterSlot" in caplog.text
    assert "private-player-name" not in caplog.text


def test_roster_fallback_reads_team_slots_without_race_templates():
    @dataclass
    class Roster:
        players: tuple
        raw_xml: str

    roster = Roster(
        players=(),
        raw_xml=(
            "<ResponseGetTeamRoster><Roster><RaceRoster><Slots><RosterSlot><Lines>"
            "<RosterSlotLine><Player><Name>Template Player</Name></Player></RosterSlotLine>"
            "</Lines></RosterSlot></Slots></RaceRoster><Slots><TeamRosterSlot>"
            "<Number>6</Number><Player><Id>player-1</Id><Name>Current Player</Name>"
            "<Position>55</Position><Number>3</Number><Level>2</Level><Spp>4</Spp>"
            "<Value>70000</Value></Player></TeamRosterSlot></Slots></Roster>"
            "</ResponseGetTeamRoster>"
        ),
    )

    result = team_api._roster_with_skill_names(roster)

    assert len(result["players"]) == 1
    assert result["players"][0]["name"] == "Current Player"
    assert result["players"][0]["position_id"] == 55
    assert result["players"][0]["slot_number"] == 6
    assert result["players"][0]["number"] == 3
    assert result["players"][0]["spp"] == 4


def test_owned_player_finds_player_from_actual_team_roster_slots():
    @dataclass
    class Roster:
        players: tuple
        raw_xml: str

    client = FakeClient({0: (["team-1"], 1)})
    roster = Roster(
        players=(),
        raw_xml=(
            "<ResponseGetTeamRoster><Roster><Slots><TeamRosterSlot><Number>6</Number>"
            "<Player><Id>player-1</Id><Name>Current Player</Name><Number>3</Number>"
            "<Spp>8</Spp></Player></TeamRosterSlot></Slots></Roster></ResponseGetTeamRoster>"
        ),
    )
    client.get_team_roster_model = lambda _team_id: roster

    found_roster, player = team_api.owned_player(client, "team-1", "player-1")

    assert found_roster is roster
    assert player.player_id == "player-1"
    assert player.spp == 8


def test_owned_player_requires_membership_in_requested_team():
    player = SimpleNamespace(player_id="player-1")
    client = FakeClient({0: (["team-1"], 1)}, players=[player])

    with pytest.raises(HTTPException) as error:
        team_api.owned_player(client, "team-1", "player-2")

    assert error.value.status_code == 404


def test_unavailable_skill_is_rejected_without_calling_bb3_mutation(monkeypatch):
    player = SimpleNamespace(player_id="player-1", spp=10)
    client = FakeClient({0: (["team-1"], 1)}, players=[player])
    monkeypatch.setattr(team_api.session_manager, "call", lambda _owner, _session, operation: operation(client))

    with pytest.raises(HTTPException) as error:
        team_api.advance_skill(
            "session-1",
            "team-1",
            "player-1",
            SkillAdvancementRequest(kind="chosen", skillId=7),
            owner="owner-1",
        )

    assert error.value.status_code == 400
    assert client.skill_mutations == []


def test_roster_skill_names_resolve_from_game_rules_and_keep_unknown_ids(tmp_path, monkeypatch):
    rules_path = tmp_path / "BB3Rules.json"
    rules_path.write_text(json.dumps({
        "bb3_rules_skill": [
            {"code": 30, "data": "block"},
        ],
    }))
    monkeypatch.setattr(team_api, "_bb3_rules", lambda: BB3Rules.load(rules_path))
    roster = {"players": [{"skill_ids": [30, 999]}]}

    result = team_api._roster_with_skill_names(roster)

    assert result["players"][0]["skill_ids"] == [30, 999]
    assert result["players"][0]["skill_names"] == {"30": "block"}


def test_skill_name_is_unresolved_without_rules_file(monkeypatch):
    monkeypatch.setattr(team_api, "_bb3_rules", lambda: None)

    assert team_api._skill_name(30) is None


def test_improvement_choices_include_game_rule_skill_names(tmp_path, monkeypatch):
    rules_path = tmp_path / "BB3Rules.json"
    rules_path.write_text(json.dumps({
        "bb3_rules_skill": [
            {"code": 30, "data": "block"},
        ],
    }))
    monkeypatch.setattr(team_api, "_bb3_rules", lambda: BB3Rules.load(rules_path))
    improvements = {"skill_categories": [{"skills": [{"skill_id": 30}]}]}

    result = team_api._improvements_with_skill_names(improvements)

    assert result["skill_categories"][0]["skills"][0]["name"] == "block"