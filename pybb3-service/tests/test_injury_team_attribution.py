import base64

from app.services.replay_parser import parse_replay


def _message(xml: str) -> str:
    return base64.b64encode(xml.encode()).decode()


def test_armour_and_injury_dice_follow_affected_player_team_not_active_team():
    step_home_hits_away = _message(
        "<StepBlock><PlayerId>10</PlayerId><TargetId>20</TargetId></StepBlock>"
    )
    armour_away = _message(
        "<ResultRoll><RollType>10</RollType><Dice>"
        "<Die><DieType>0</DieType><Value>5</Value></Die>"
        "<Die><DieType>0</DieType><Value>6</Value></Die>"
        "</Dice></ResultRoll>"
    )
    injury_away = _message(
        "<ResultRoll><RollType>11</RollType><Dice>"
        "<Die><DieType>0</DieType><Value>4</Value></Die>"
        "<Die><DieType>0</DieType><Value>5</Value></Die>"
        "</Dice></ResultRoll>"
    )
    step_away_hits_home = _message(
        "<StepBlock><PlayerId>20</PlayerId><TargetId>10</TargetId></StepBlock>"
    )
    armour_home = _message(
        "<ResultRoll><RollType>10</RollType><Dice>"
        "<Die><DieType>0</DieType><Value>3</Value></Die>"
        "<Die><DieType>0</DieType><Value>4</Value></Die>"
        "</Dice></ResultRoll>"
    )
    injury_home = _message(
        "<ResultRoll><RollType>11</RollType><Dice>"
        "<Die><DieType>0</DieType><Value>2</Value></Die>"
        "<Die><DieType>0</DieType><Value>3</Value></Die>"
        "</Dice></ResultRoll>"
    )

    xml = f"""<Replay>
      <NotificationGameJoined><GameInfos><GamersInfos>
        <GamerInfos><Slot>0</Slot><Roster><Players>
          <PlayerData><LobbyId>aG9tZQ==</LobbyId><Name>SG9tZSBQbGF5ZXI=</Name><Number>1</Number></PlayerData>
        </Players></Roster></GamerInfos>
        <GamerInfos><Slot>1</Slot><Roster><Players>
          <PlayerData><LobbyId>YXdheQ==</LobbyId><Name>QXdheSBQbGF5ZXI=</Name><Number>2</Number></PlayerData>
        </Players></Roster></GamerInfos>
      </GamersInfos></GameInfos></NotificationGameJoined>
      <ReplayStep>
        <Clock>1</Clock>
        <BoardState><ActiveTeam>0</ActiveTeam><ListTeams>
          <TeamState><GameTurn>1</GameTurn><ListPitchPlayers>
            <PlayerState><Id>10</Id><Data><LobbyId>aG9tZQ==</LobbyId></Data></PlayerState>
          </ListPitchPlayers></TeamState>
          <TeamState><GameTurn>1</GameTurn><ListPitchPlayers>
            <PlayerState><Id>20</Id><Data><LobbyId>YXdheQ==</LobbyId></Data></PlayerState>
          </ListPitchPlayers></TeamState>
        </ListTeams></BoardState>
        <EventExecuteSequence><Sequence>
          <StepResult>
            <Step><MessageData>{step_home_hits_away}</MessageData></Step>
            <Results>
              <StringMessage><MessageData>{armour_away}</MessageData></StringMessage>
              <StringMessage><MessageData>{injury_away}</MessageData></StringMessage>
            </Results>
          </StepResult>
          <StepResult>
            <Step><MessageData>{step_away_hits_home}</MessageData></Step>
            <Results>
              <StringMessage><MessageData>{armour_home}</MessageData></StringMessage>
              <StringMessage><MessageData>{injury_home}</MessageData></StringMessage>
            </Results>
          </StepResult>
        </Sequence></EventExecuteSequence>
      </ReplayStep>
    </Replay>""".encode()

    analysis = parse_replay(xml)["analysis"]
    rows = [row for row in analysis["diceStatistics"] if row["label"] in {"Armour", "Injury"}]
    by_key = {(row["label"], row["teamId"]): row for row in rows}

    assert by_key[("Armour", 0)]["resultCounts"] == {"7": 1}
    assert by_key[("Armour", 1)]["resultCounts"] == {"11": 1}
    assert by_key[("Injury", 0)]["resultCounts"] == {"5": 1}
    assert by_key[("Injury", 1)]["resultCounts"] == {"9": 1}
