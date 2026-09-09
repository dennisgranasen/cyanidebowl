import base64
import xml.etree.ElementTree as ET

from app.services.replay_player_identity import build_player_index


def _b64(value: str) -> str:
    return base64.b64encode(value.encode()).decode()


def test_initial_board_state_resolves_real_replay_numeric_ids_to_names():
    vollo_lobby = _b64("e21e3c65-974f-11f1-a124-bc2411305479")
    nilvurr_lobby = _b64("e3770582-974f-11f1-a124-bc2411305479")

    root = ET.fromstring(f"""<Replay>
      <NotificationGameJoined>
        <InitialBoardState><ListTeams>
          <TeamState/>
          <TeamState><ListPitchPlayers>
            <PlayerState>
              <Id>37</Id>
              <Data>
                <Id>37</Id>
                <Name>{_b64("Vollo 'Barrel'")}</Name>
                <LobbyId>{vollo_lobby}</LobbyId>
                <Number>1</Number>
              </Data>
            </PlayerState>
            <PlayerState>
              <Id>43</Id>
              <Data>
                <Id>43</Id>
                <Name>{_b64("Nilvurr 'Glutton'")}</Name>
                <LobbyId>{nilvurr_lobby}</LobbyId>
                <Number>8</Number>
              </Data>
            </PlayerState>
            <PlayerState>
              <Id>47</Id>
              <Data>
                <Id>47</Id>
                <Name>{_b64("Ugorh")}</Name>
                <Number>6</Number>
              </Data>
            </PlayerState>
          </ListPitchPlayers></TeamState>
        </ListTeams></InitialBoardState>
      </NotificationGameJoined>

      <ReplayStep>
        <BoardState><ListTeams>
          <TeamState/>
          <TeamState><ListPitchPlayers>
            <PlayerState><Id>37</Id><Data><Id>37</Id></Data></PlayerState>
            <PlayerState><Id>43</Id><Data><Id>43</Id></Data></PlayerState>
            <PlayerState><Id>47</Id><Data><Id>47</Id></Data></PlayerState>
          </ListPitchPlayers></TeamState>
        </ListTeams></BoardState>
      </ReplayStep>
    </Replay>""")

    players = build_player_index(root)

    assert players[37]["name"] == "Vollo 'Barrel'"
    assert players[37]["teamId"] == 1
    assert players[37]["stablePlayerId"] == "e21e3c65-974f-11f1-a124-bc2411305479"

    assert players[43]["name"] == "Nilvurr 'Glutton'"
    assert players[43]["teamId"] == 1
    assert players[43]["stablePlayerId"] == "e3770582-974f-11f1-a124-bc2411305479"

    assert players[47]["name"] == "Ugorh"
    assert players[47]["teamId"] == 1
