import base64
import xml.etree.ElementTree as ET

from app.services.replay_player_identity import build_player_index, enrich_match_event


def _b64(value: str) -> str:
    return base64.b64encode(value.encode()).decode()


def test_player_names_survive_non_pitch_state_and_data_id_alias():
    lobby = _b64("player-guid")
    root = ET.fromstring(f"""<Replay>
      <NotificationGameJoined><GameInfos><GamersInfos>
        <GamerInfos><Slot>1</Slot><Roster><Players>
          <PlayerData><LobbyId>{lobby}</LobbyId><Name>{_b64("Morg's Apprentice")}</Name><Number>9</Number></PlayerData>
        </Players></Roster></GamerInfos>
      </GamersInfos></GameInfos></NotificationGameJoined>
      <ReplayStep><BoardState><ListTeams>
        <TeamState/>
        <TeamState><Dugout><KnockedOutPlayers>
          <PlayerState><Id>37</Id><Data><Id>137</Id><LobbyId>{lobby}</LobbyId><Name>{_b64("Morg's Apprentice")}</Name></Data></PlayerState>
        </KnockedOutPlayers></Dugout></TeamState>
      </ListTeams></BoardState></ReplayStep>
    </Replay>""")

    players = build_player_index(root)
    assert players[37]["name"] == "Morg's Apprentice"
    assert players[37]["teamId"] == 1
    assert players[137]["name"] == "Morg's Apprentice"
    assert players[137]["teamId"] == 1


def test_casualty_exposes_actor_and_affected_names():
    players = {
        10: {"teamId": 0, "name": "Bone Cruncher", "number": 4},
        20: {"teamId": 1, "name": "Fast Feet", "number": 7},
    }
    raw = ET.fromstring("<EventCasualty><AttackerId>10</AttackerId><PlayerId>20</PlayerId></EventCasualty>")
    item = {"teamId": None, "playerId": 10, "details": {}}

    enrich_match_event(
        item, raw, "CASUALTY", players,
        actor_player_id=10, affected_player_id=20,
    )

    assert item["actorPlayerName"] == "Bone Cruncher"
    assert item["affectedPlayerName"] == "Fast Feet"
    assert item["teamId"] == 0
    assert item["details"]["causingPlayerName"] == "Bone Cruncher"
    assert item["details"]["injuredPlayerName"] == "Fast Feet"
