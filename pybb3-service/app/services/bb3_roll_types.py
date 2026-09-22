"""Blood Bowl 3 replay RollType identifiers.

Interoperability reference:
    sjogrenm/ZFLStats, ``BloodBowl3/RollType.cs``
    https://github.com/sjogrenm/ZFLStats/blob/main/BloodBowl3/RollType.cs

The numeric mapping below was derived from that independent open-source
project. CyanideBowl/BlaskScore is not affiliated with or endorsed by
ZFLStats. Keep this provenance notice with the mapping.

IMPORTANT: these values are specific to the Blood Bowl 3 replay protocol.
Blood Bowl 2 uses a different replay format and MUST have its own decoder and
mapping; do not reuse this enum in the BB2 decoder.
"""
from __future__ import annotations

from enum import IntEnum


class Bb3RollType(IntEnum):
    NoRoll = 0
    GFI = 1
    Dodge = 2
    Block = 3
    PickUp = 4
    Pass = 5
    Interception = 6
    Catch = 7
    Scatter = 8
    ThrowIn = 9
    Armor = 10
    Injury = 11
    Casualty = 12
    WakeUp = 13
    HalflingChef = 14
    Pro = 15
    Tentacles = 16
    ScatterPlayer = 17
    SwelteringHeat = 18
    PenaltyShots = 19
    StandUp = 20
    Bribe = 21
    BrilliantCoaching = 22
    FanFactor = 23
    Meteo = 24
    Bounce = 25
    Deviate = 26
    TouchBack = 27
    BadHabits = 28
    JumpOver = 29
    ArgueTheCall = 30
    Dauntless = 31
    JumpUp = 32
    BoneHead = 33
    ReallyStupid = 34
    UnchannelledFury = 35
    AnimalSavagery = 36
    FoulAppearance = 37
    SeismicActivityTrigger = 38
    Unstun = 39
    Geyser = 40
    ThrowTeamMate = 41
    Land = 42
    AlwaysHungry = 43
    EscapeTeamMate = 44
    VomitAccuracy = 45
    Regeneration = 46
    BloomingGarden = 47
    FireballHit = 48
    ThunderboltHit = 49
    StrangeFaunaHit = 50
    Zap = 51
    KickOffTable = 52
    CheeringFans = 53
    OfficiousRefRollOff = 54
    OfficiousRefAttack = 55
    SeismicActivityRollOff = 56
    Brawler = 57
    LastingInjury = 58
    PitchInvasion = 59
    GoblinPitch = 60
    TreacherousTrap = 61
    ForWhomTheBellTolls = 62
    AssassinationAttempt = 63
    BadBurger = 64
    TheProtege = 65
    HypnoticGaze = 66
    Chainsaw = 67
    TakeRoot = 68
    ClosedScrutiny = 69
    ThrowARock = 70
    Loner = 71
    WarpstoneDust = 72
    Shadowing = 73
    Animosity = 74
    Swarming = 75
    DedicatedFansChange = 76
    DedicatedFansConcessionLoss = 77
    DeviationDistance = 78
    ThrowInDistance = 79
    ActivationNumber = 80
    PitchInvasionNumber = 81
    BadHabitsPrayer = 82
    SwelteringHeatNumber = 83
    PrayersTable = 84
    BallAndChainDirection = 87
    BombExplosionHit = 88
    CindySpecial = 91
    Bloodlust = 96


_DISPLAY_OVERRIDES = {
    Bb3RollType.GFI: "Rush",
    Bb3RollType.PickUp: "Pick Up",
    Bb3RollType.ThrowIn: "Throw In",
    Bb3RollType.JumpOver: "Jump Over",
    Bb3RollType.ArgueTheCall: "Argue the Call",
    Bb3RollType.ThrowTeamMate: "Throw Team-Mate",
    Bb3RollType.Land: "Landing",
}

_ACTION = {
    Bb3RollType.GFI, Bb3RollType.Dodge, Bb3RollType.PickUp,
    Bb3RollType.Pass, Bb3RollType.Interception, Bb3RollType.Catch,
    Bb3RollType.StandUp, Bb3RollType.JumpOver, Bb3RollType.ThrowTeamMate,
    Bb3RollType.Land, Bb3RollType.EscapeTeamMate, Bb3RollType.VomitAccuracy,
    Bb3RollType.BombExplosionHit,
}

_EXCLUDED_FROM_ACTION_STATS = {Bb3RollType.BallAndChainDirection}

_SKILL_TRAIT = {
    Bb3RollType.Pro, Bb3RollType.Tentacles, Bb3RollType.Dauntless,
    Bb3RollType.JumpUp, Bb3RollType.BoneHead, Bb3RollType.ReallyStupid,
    Bb3RollType.UnchannelledFury, Bb3RollType.AnimalSavagery,
    Bb3RollType.FoulAppearance, Bb3RollType.Brawler,
    Bb3RollType.HypnoticGaze, Bb3RollType.Chainsaw, Bb3RollType.TakeRoot,
    Bb3RollType.Loner, Bb3RollType.Shadowing, Bb3RollType.Animosity,
    Bb3RollType.Bloodlust,
}

_INJURY_RECOVERY = {
    Bb3RollType.Armor, Bb3RollType.Injury, Bb3RollType.Casualty,
    Bb3RollType.WakeUp, Bb3RollType.Unstun, Bb3RollType.Regeneration,
    Bb3RollType.LastingInjury,
}


def bb3_roll_name(value: int) -> str:
    """Return a stable human-readable name without guessing unknown values."""
    try:
        roll_type = Bb3RollType(value)
    except ValueError:
        return f"BB3 RollType {value}"
    if roll_type in _DISPLAY_OVERRIDES:
        return _DISPLAY_OVERRIDES[roll_type]
    name = roll_type.name
    out = []
    for index, character in enumerate(name):
        if index and character.isupper() and not name[index - 1].isupper():
            out.append(" ")
        out.append(character)
    return "".join(out)


def bb3_roll_category(value: int) -> str:
    """Coarse presentation category; protocol identity remains sourceRollType."""
    try:
        roll_type = Bb3RollType(value)
    except ValueError:
        return "unknown"
    if roll_type in _ACTION:
        return "action"
    if roll_type in _SKILL_TRAIT:
        return "skillTrait"
    if roll_type in _INJURY_RECOVERY:
        return "injuryRecovery"
    return "system"


def bb3_roll_counts_as_action_stat(value: int) -> bool:
    """Return whether a D6 ResultRoll represents a success/failure attempt."""
    try:
        roll_type = Bb3RollType(value)
    except ValueError:
        # Preserve the previous behaviour for unknown protocol values.
        return True
    return roll_type not in _EXCLUDED_FROM_ACTION_STATS
