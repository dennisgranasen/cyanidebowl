from __future__ import annotations

from dataclasses import asdict, dataclass, field
from enum import Enum
from typing import Any


class BlockChooser(str, Enum):
    ATTACKER = "ATTACKER"
    DEFENDER = "DEFENDER"


class BlockOutcome(str, Enum):
    SUCCESS = "success"
    NEUTRAL = "neutral"
    FAILURE = "fail"
    UNKNOWN = "unknown"


class SpecialOutcome(str, Enum):
    SUCCESS = "success"
    NEUTRAL = "neutral"
    FAILURE = "fail"
    UNKNOWN = "unknown"


BLOCK_FACE_NAMES = {
    0: "skull",
    1: "bothDown",
    2: "push",
    3: "tackle",
    4: "defenderDown",
}


@dataclass(slots=True)
class D6Action:
    action_id: str
    action_type: str
    team_id: Any
    player_id: Any
    target_id: Any
    target: int
    rolls: list[int] = field(default_factory=list)
    successful: bool = False
    reroll_used: bool = False
    source: str = "BB3"

    @property
    def kind(self) -> str:
        return "d6"

    def to_dict(self) -> dict[str, Any]:
        result = asdict(self)
        result["kind"] = self.kind
        return _camel(result)


@dataclass(slots=True)
class BlockAction:
    action_id: str
    attacker_team_id: Any
    attacker_player_id: Any
    defender_player_id: Any
    dice_count: int | None = None
    chooser: BlockChooser = BlockChooser.ATTACKER
    rolled_faces: list[str] = field(default_factory=list)
    selected_face: str = "unknown"
    outcome: BlockOutcome = BlockOutcome.UNKNOWN
    attacker_down: bool | None = None
    defender_down: bool | None = None
    defender_surfed: bool = False
    source: str = "BB3"

    @property
    def kind(self) -> str:
        return "block"

    @property
    def dice_label(self) -> str:
        if not self.dice_count:
            return "Unknown dice"
        prefix = "-" if self.chooser is BlockChooser.DEFENDER else ""
        return f"{prefix}{self.dice_count}D"

    def to_dict(self) -> dict[str, Any]:
        result = asdict(self)
        result["chooser"] = self.chooser.value
        result["outcome"] = self.outcome.value
        result["kind"] = self.kind
        result["dice_label"] = self.dice_label
        return _camel(result)


@dataclass(slots=True)
class SpecialAction:
    """Canonical action for mechanics that must not be folded into D6/block stats.

    `raw_events` intentionally keeps the decoded BB3 message fragments used to
    construct the action. Special mechanics evolve quickly, so retaining the
    source messages lets us enrich the typed model later without losing data.
    """

    action_id: str
    team_id: Any = None
    player_id: Any = None
    target_team_id: Any = None
    target_player_id: Any = None
    rolls: list[int] = field(default_factory=list)
    outcome: SpecialOutcome = SpecialOutcome.UNKNOWN
    details: dict[str, Any] = field(default_factory=dict)
    raw_events: list[dict[str, Any]] = field(default_factory=list)
    source: str = "BB3"
    action_type: str = field(init=False, default="Special")

    @property
    def kind(self) -> str:
        return "special"

    def to_dict(self) -> dict[str, Any]:
        result = asdict(self)
        result["outcome"] = self.outcome.value
        result["kind"] = self.kind
        return _camel(result)


@dataclass(slots=True)
class StabAction(SpecialAction):
    action_type: str = field(init=False, default="Stab")


@dataclass(slots=True)
class WizardAction(SpecialAction):
    action_type: str = field(init=False, default="Wizard")


@dataclass(slots=True)
class BombAction(SpecialAction):
    action_type: str = field(init=False, default="Bomb")


@dataclass(slots=True)
class ChainsawAction(SpecialAction):
    action_type: str = field(init=False, default="Chainsaw")


@dataclass(slots=True)
class FoulAction(SpecialAction):
    action_type: str = field(init=False, default="Foul")


@dataclass(slots=True)
class ThrowTeamMateAction(SpecialAction):
    action_type: str = field(init=False, default="Throw Team-Mate")


@dataclass(slots=True)
class KickTeamMateAction(SpecialAction):
    action_type: str = field(init=False, default="Kick Team-Mate")


@dataclass(slots=True)
class HypnoticGazeAction(SpecialAction):
    action_type: str = field(init=False, default="Hypnotic Gaze")


@dataclass(slots=True)
class ProjectileVomitAction(SpecialAction):
    action_type: str = field(init=False, default="Projectile Vomit")


@dataclass(slots=True)
class BloodlustAction(SpecialAction):
    action_type: str = field(init=False, default="Bloodlust")


def _camel(values: dict[str, Any]) -> dict[str, Any]:
    converted = {}
    for key, value in values.items():
        parts = key.split("_")
        converted[parts[0] + "".join(part.title() for part in parts[1:])] = value
    return converted
