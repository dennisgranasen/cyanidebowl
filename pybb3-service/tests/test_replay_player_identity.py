import base64
import xml.etree.ElementTree as ET

from app.services.replay_player_identity import build_player_index, enrich_match_event


def _b64(value: str) -> str:
    return base64.b64encode(value.encode()).decode()


def _fixture() -> ET.Element:
    home_lobby = _b64("home-player-guid")
    away_lobby = _b64("away-player-guid")
    return ET.fromstring(f"""<Replay>
      <NotificationGameJoined><GameInfos><GamersInfos>
        <GamerInfos><Slot>0</Slot><Name>{_b64("Home Coach")}</Name><Roster><Players>
          <PlayerData><LobbyId>{home_lobby}</LobbyId><Name>{_b64("Grimfang Bonebreaker")}</Name><Number>4</Number></PlayerData>
        </Players></Roster></GamerInfos>
        <GamerInfos><Slot>1</Slot><Name>{_b64("Away Coach")}</Name><Roster><Players>
          <PlayerData><LobbyId>{away_lobby}</LobbyId><Name>{_b64("Silas Quickstep")}</Name><Number>7</Number></PlayerData>
        </Players></Roster></GamerInfos>
      </GamersInfos></GameInfos></NotificationGameJoined>
      <ReplayStep><BoardState><ListTeams>
        <TeamState><ListPitchPlayers>
          <PlayerState><Id>10</Id><Data><LobbyId>{home_lobby}</LobbyId></Data></PlayerState>
        </ListPitchPlayers></TeamState>
        <TeamState><ListPitchPlayers>
          <PlayerState><Id>20</Id><Data><LobbyId>{away_lobby}</LobbyId></Data></PlayerState>
        </ListPitchPlayers></TeamState>
      </ListTeams></BoardState></ReplayStep>
    </Replay>""")


def test_runtime_ids_resolve_to_roster_player_names_and_teams():
    players = build_player_index(_fixture())

    assert players[10]["name"] == "Grimfang Bonebreaker"
    assert players[10]["number"] == 4
    assert players[10]["teamId"] == 0
    assert players[20]["name"] == "Silas Quickstep"
    assert players[20]["teamId"] == 1


def test_casualty_roles_use_player_names_not_coach_names():
    players = build_player_index(_fixture())
    raw = ET.fromstring("<EventCasualty><AttackerId>10</AttackerId><PlayerId>20</PlayerId></EventCasualty>")
    item = {"teamId": None, "playerId": 10, "details": {}}

    enrich_match_event(item, raw, "CASUALTY", players)

    assert item["teamId"] == 0
    assert item["playerName"] == "Grimfang Bonebreaker"
    assert item["details"]["causingPlayerName"] == "Grimfang Bonebreaker"
    assert item["details"]["injuredPlayerName"] == "Silas Quickstep"
    assert item["details"]["targetPlayerName"] == "Silas Quickstep"
    assert "Home Coach" not in str(item)
    assert "Away Coach" not in str(item)


def test_touchdown_exposes_scorer_name():
    players = build_player_index(_fixture())
    raw = ET.fromstring("<EventTouchdown><PlayerId>10</PlayerId></EventTouchdown>")
    item = {"teamId": None, "playerId": 10, "details": {}}

    enrich_match_event(item, raw, "TOUCHDOWN", players)

    assert item["teamId"] == 0
    assert item["playerName"] == "Grimfang Bonebreaker"
    assert item["details"]["scorerName"] == "Grimfang Bonebreaker"
