import xml.etree.ElementTree as ET

from app.services.replay_timeline import _dice_values, _first


def test_real_shape_apothecary_choice_exposes_original_and_new_casualty_results():
    event = ET.fromstring(
        "<QuestionApothecaryCasualtyChoice>"
        "<GamerId>0</GamerId>"
        "<NewRoll><Dice><Die><DieType>4</DieType><Value>14</Value></Die>"
        "<Die><DieType>0</DieType><Value>6</Value></Die></Dice>"
        "<RollType>12</RollType><Outcome>9</Outcome></NewRoll>"
        "<OriginalRoll><Dice><Die><DieType>4</DieType><Value>16</Value></Die></Dice>"
        "<RollType>12</RollType><Outcome>10</Outcome></OriginalRoll>"
        "</QuestionApothecaryCasualtyChoice>"
    )

    original = event.find(".//OriginalRoll")
    new_roll = event.find(".//NewRoll")

    assert _first(original, ("Outcome",)) == 10
    assert _dice_values(original) == [16]
    assert _first(new_roll, ("Outcome",)) == 9
    assert _dice_values(new_roll) == [14, 6]


def test_real_shape_result_apothecary_exposes_chosen_casualty():
    event = ET.fromstring(
        "<ResultApothecary>"
        "<PlayerStatus>5</PlayerStatus><ApothecaryUsed>1</ApothecaryUsed>"
        "<Casualty>10</Casualty><IsInjury>0</IsInjury>"
        "</ResultApothecary>"
    )

    assert _first(event, ("Casualty",)) == 10
