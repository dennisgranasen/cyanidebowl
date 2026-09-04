import gzip
import json

from app.services.replay_parser import parse_replay


REPLAY = b"""<Replay>
<ReplayVersion>1-4-0-0</ReplayVersion>
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
    assert len(analysis["diceRolls"]) == 3
    assert analysis["dieValueCounts"]["UNKNOWN:6"] == 1
    assert analysis["dieValueCounts"]["3:1"] == 1
    assert analysis["resourceEvents"][0]["eventType"] == "EventUseTeamReroll"
    assert analysis["specialEvents"][0]["eventType"] == "EventRegeneration"
    assert analysis["specialEvents"][0]["success"] is True
    assert analysis["checkpointCount"] == 2
    assert "checkpoint" in compact["steps"][0]
    assert "checkpoint" not in compact["steps"][1]
    assert compact["steps"][2]["checkpoint"]["context"]["teamTurns"][1]["gameTurn"] == 1
