import gzip
import io
import json
import zipfile

from app.services.replay_parser import parse_replay, parse_replay_artifact


REPLAY = b"""<Replay>
<ReplayVersion>1-4-0-0</ReplayVersion>
<NotificationGameJoined><GameInfos><Competition><CompetitionInfos><MatchId>dGVzdC1tYXRjaC1pZA==</MatchId></CompetitionInfos></Competition></GameInfos></NotificationGameJoined>
<ReplayStep><Clock>100</Clock>
 <EventWeatherRoll><Dice><Die><Value>6</Value></Die><Die><Value>4</Value></Die></Dice></EventWeatherRoll>
 <BoardState><CurrentPhase>4</CurrentPhase><ActiveTeam>0</ActiveTeam><ListTeams>
  <TeamState><GameTurn>1</GameTurn><Data><TeamId>0</TeamId></Data></TeamState>
  <TeamState><GameTurn>0</GameTurn><Data><TeamId>1</TeamId></Data></TeamState>
 </ListTeams></BoardState></ReplayStep>
<ReplayStep><Clock>200</Clock>
 <EventUseTeamReroll><PlayerId>42</PlayerId><Dice><Die><DieType>3</DieType><Value>1</Value></Die></Dice><Outcome>0</Outcome></EventUseTeamReroll>
 <EventRegeneration><PlayerId>42</PlayerId><Dice><Die><DieType>3</DieType><Value>5</Value></Die></Dice><Outcome>1</Outcome><Success>1</Success></EventRegeneration>
 <BoardState><CurrentPhase>4</CurrentPhase><ActiveTeam>0</ActiveTeam><ListTeams>
  <TeamState><GameTurn>1</GameTurn><Data><TeamId>0</TeamId></Data></TeamState>
  <TeamState><GameTurn>0</GameTurn><Data><TeamId>1</TeamId></Data></TeamState>
 </ListTeams></BoardState></ReplayStep>
<ReplayStep><Clock>300</Clock><EventNewTurn/><BoardState><CurrentPhase>4</CurrentPhase><ActiveTeam>1</ActiveTeam><ListTeams>
  <TeamState><GameTurn>1</GameTurn><Data><TeamId>0</TeamId></Data></TeamState>
  <TeamState><GameTurn>1</GameTurn><Data><TeamId>1</TeamId></Data></TeamState>
 </ListTeams></BoardState></ReplayStep>
</Replay>"""


def test_extracts_dice_resources_special_events_and_semantic_checkpoints():
    result = parse_replay(REPLAY)
    analysis = result["analysis"]
    compact = json.loads(gzip.decompress(result["compactGzip"]))

    assert analysis["replayVersion"] == "1-4-0-0"
    assert analysis["sourceMatchId"] == "test-match-id"
    assert analysis["analysisConfidence"] == "CANONICAL_ACTIONS"
    assert analysis["sourceFormat"] == "BB3"
    assert analysis["canonicalActions"] == []
    assert analysis["actionStatistics"] == []
    assert len(analysis["diceRolls"]) == 3
    # Weather omits DieType in BB3, but event context identifies it as D6.
    assert analysis["dieValueCounts"]["0:6"] == 1
    assert analysis["dieValueCounts"]["3:1"] == 1
    weather = next(row for row in analysis["diceStatistics"] if row["label"] == "Weather")
    assert weather["dieTypeName"] == "D6"
    assert weather["teamId"] is None
    assert weather["inferred"] is True
    assert weather["rolls"] == [[6, 4]]
    assert weather["resultCounts"] == {"10": 1}
    assert analysis["resourceEvents"][0]["eventType"] == "EventUseTeamReroll"
    assert analysis["specialEvents"][0]["eventType"] == "EventRegeneration"
    assert analysis["specialEvents"][0]["success"] is True
    assert analysis["checkpointCount"] == 2
    assert "checkpoint" in compact["steps"][0]
    assert "checkpoint" not in compact["steps"][1]
    assert compact["steps"][2]["checkpoint"]["context"]["teamTurns"][1]["gameTurn"] == 1
    assert compact["sourceFormat"] == "BB3"
    assert compact["canonicalActions"] == []


def test_fan_factor_d3_keeps_home_away_team_context():
    replay = b"""<Replay><ReplayStep><Clock>1</Clock>
    <EventFanFactor>
      <HomeRoll><Dice><Die><DieType>3</DieType><Value>1</Value></Die></Dice><RollType>23</RollType></HomeRoll>
      <AwayRoll><Dice><Die><DieType>3</DieType><Value>3</Value></Die></Dice><RollType>23</RollType></AwayRoll>
    </EventFanFactor>
    </ReplayStep></Replay>"""
    analysis = parse_replay(replay)["analysis"]
    fan = [row for row in analysis["diceStatistics"] if row["label"] == "Fan Factor"]
    assert [row["teamId"] for row in fan] == [0, 1]
    assert all(row["dieTypeName"] == "D3" for row in fan)
    assert [row["rolls"] for row in fan] == [[[1]], [[3]]]


def test_explicit_d6_zero_is_not_misreported_as_unknown():
    replay = b"""<Replay><ReplayStep><Clock>1</Clock>
    <EventFoo><Dice><Die><DieType>0</DieType><Value>4</Value></Die></Dice></EventFoo>
    </ReplayStep></Replay>"""
    analysis = parse_replay(replay)["analysis"]
    assert analysis["dieValueCounts"]["0:4"] == 1
    assert "UNKNOWN:4" not in analysis["dieValueCounts"]


def test_casualty_result_tag_infers_d16_without_adding_multiple_values():
    replay = b"""<Replay><ReplayStep><Clock>1</Clock>
    <ResultCasualtyRoll><Dice>
      <Die><Value>13</Value></Die><Die><Value>3</Value></Die>
    </Dice></ResultCasualtyRoll>
    </ReplayStep></Replay>"""
    analysis = parse_replay(replay)["analysis"]
    casualty = next(row for row in analysis["diceStatistics"] if row["label"] == "Casualty")
    assert casualty["dieTypeName"] == "D16"
    assert casualty["inferred"] is True
    assert casualty["rolls"] == [[13, 3]]
    assert casualty["resultCounts"] == {"13": 1, "3": 1}
    assert "16" not in casualty["resultCounts"]


def test_block_dice_are_not_classified_as_other_replay_dice():
    replay = b"""<Replay><ReplayStep><Clock>1</Clock>
    <ResultBlockRoll><RollType>3</RollType><Dice>
      <Die><DieType>2</DieType><Value>4</Value></Die>
    </Dice></ResultBlockRoll>
    </ReplayStep></Replay>"""
    analysis = parse_replay(replay)["analysis"]
    block = next(row for row in analysis["diceStatistics"] if row["label"] == "Block")
    assert block["category"] == "block"


def test_parse_replay_artifact_accepts_bb2_bbrz():
    replay = b"<Replay><ReplayStep><GameInfos><Id>bb2-test</Id></GameInfos></ReplayStep></Replay>"
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, "w") as archive:
        archive.writestr("replay.xml", replay)

    format_name, xml, result = parse_replay_artifact(stream.getvalue())
    assert format_name == "BBRZ"
    assert xml == replay
    assert result["analysis"]["sourceFormat"] == "BB2"
    assert result["analysis"]["analysisConfidence"] == "RAW_BB2"
