import xml.etree.ElementTree as ET

from app.services.replay_timeline import build_match_events


def test_weather_is_match_level_and_preserves_2d6_expression_data():
    root = ET.fromstring("""<Replay><ReplayStep><Clock>10</Clock>
      <EventWeatherRoll><Dice>
        <Die><DieType>0</DieType><Value>2</Value></Die>
        <Die><DieType>0</DieType><Value>3</Value></Die>
      </Dice></EventWeatherRoll>
      <BoardState><ActiveTeam>1</ActiveTeam></BoardState>
    </ReplayStep></Replay>""")
    weather = next(event for event in build_match_events(root) if event["type"] == "WEATHER")
    assert weather["teamId"] is None
    assert weather["details"]["dice"] == [2, 3]
    assert weather["details"]["rawTotal"] == 5
    assert weather["details"]["modifiedTotal"] == 5
    assert weather["details"]["tableName"] == "Standard"
    assert weather["details"]["weather"] == "Perfect Conditions"


def test_kickoff_resolution_is_linked_to_the_kickoff_event():
    root = ET.fromstring("""<Replay><ReplayStep><Clock>10</Clock>
      <EventKickOffTable><KickingTeamId>0</KickingTeamId><Dice>
        <Die><Value>3</Value></Die><Die><Value>4</Value></Die>
      </Dice><Result>7</Result></EventKickOffTable>
      <EventBrilliantCoaching><TeamId>0</TeamId><Dice><Die><Value>5</Value></Die></Dice></EventBrilliantCoaching>
    </ReplayStep></Replay>""")
    events = build_match_events(root)
    kickoff = next(event for event in events if event["type"] == "KICKOFF")
    coaching = next(event for event in events if event["type"] == "KICKOFF_DETAIL")
    assert kickoff["details"]["dice"] == [3, 4]
    assert kickoff["teamId"] == 0
    assert coaching["parentEventId"] == kickoff["id"]
    assert coaching["teamId"] == 0


def test_explicit_spp_events_are_timeline_events():
    root = ET.fromstring("""<Replay><ReplayStep><Clock>10</Clock>
      <EventTouchdown><TeamId>1</TeamId><PlayerId>42</PlayerId></EventTouchdown>
      <EventPassCompleted><TeamId>1</TeamId><ThrowerId>42</ThrowerId></EventPassCompleted>
      <EventInterception><TeamId>0</TeamId><PlayerId>7</PlayerId></EventInterception>
      <EventCasualty><TeamId>1</TeamId><AttackerId>42</AttackerId><PlayerId>8</PlayerId></EventCasualty>
      <EventEjection><TeamId>0</TeamId><PlayerId>9</PlayerId></EventEjection>
    </ReplayStep></Replay>""")
    events = build_match_events(root)
    by_type = {event["type"]: event for event in events}
    assert by_type["TOUCHDOWN"]["sppAwarded"] == 3
    assert by_type["COMPLETION"]["sppAwarded"] == 1
    assert by_type["INTERCEPTION"]["sppAwarded"] == 2
    assert by_type["CASUALTY"]["sppAwarded"] == 2
    assert "sppAwarded" not in by_type["EJECTION"]
