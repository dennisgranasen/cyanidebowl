"""Blood Bowl 3 replay DieType identifiers and conservative semantics.

Interoperability reference:
    sjogrenm/ZFLStats, ``BloodBowl3/DieType.cs``
    https://github.com/sjogrenm/ZFLStats/blob/main/BloodBowl3/DieType.cs

The numeric mapping below was derived from that independent open-source
project. CyanideBowl/BlaskScore is not affiliated with or endorsed by
ZFLStats. Keep this provenance notice with the mapping.

IMPORTANT: these values are specific to the Blood Bowl 3 replay protocol.
Blood Bowl 2 uses a different replay format and MUST have its own decoder and
mapping; do not reuse this enum in the BB2 decoder.

Inference below is deliberately conservative. ``sourceDieType`` remains the
wire value (or None) so inferred presentation metadata never overwrites the
original replay evidence.
"""
from __future__ import annotations

from enum import IntEnum


class Bb3DieType(IntEnum):
    D6 = 0
    D8 = 1
    Block = 2
    D3 = 3
    D16 = 4


_D8_ROLL_TYPES = {8, 9, 17, 25, 26}


def bb3_die_name(value: int | None) -> str:
    if value is None:
        return "Unknown"
    try:
        return Bb3DieType(value).name
    except ValueError:
        return f"BB3 DieType {value}"


def infer_bb3_die_type(event_type: str, roll_type: int | None, source_die_type: int | None) -> tuple[int | None, str]:
    """Return (resolved type, provenance) without changing the wire value."""
    if source_die_type is not None:
        return source_die_type, "explicit"
    if event_type == "EventWeatherRoll":
        return Bb3DieType.D6.value, "context"
    if roll_type == 23:  # FanFactor
        return Bb3DieType.D3.value, "context"
    if roll_type == 12:  # Casualty
        return Bb3DieType.D16.value, "context"
    if roll_type in _D8_ROLL_TYPES:
        return Bb3DieType.D8.value, "context"
    return None, "unknown"


def bb3_dice_semantics(event_type: str, roll_type: int | None, roll_name: str | None) -> tuple[str, str]:
    """Return a stable presentation category and label for a dice group."""
    if event_type == "EventFanFactor" or roll_type == 23:
        return "pregame", "Fan Factor"
    if event_type == "EventWeatherRoll" or roll_type == 24:
        return "pregame", "Weather"
    if roll_type == 10:
        return "injury", "Armour"
    if roll_type == 11:
        return "injury", "Injury"
    if roll_type == 12:
        return "injury", "Casualty"
    if roll_type == 46:
        return "injury", "Regeneration"
    if roll_type == 58:
        return "injury", "Lasting Injury"
    if roll_type in _D8_ROLL_TYPES:
        return "scatter", roll_name or "Scatter / direction"
    if roll_type in {1, 2, 4, 5, 6, 7, 29, 31, 32, 33, 34, 35, 36, 37, 42, 43, 66, 67, 68, 71, 73, 74}:
        return "action", roll_name or "Action roll"
    return "other", roll_name or event_type

