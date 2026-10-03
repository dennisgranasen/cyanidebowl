from typing import Literal

from pydantic import BaseModel, Field

class AddSkillRequest(BaseModel):
    player_id: str = Field(alias="playerId")
    skill_id: int = Field(alias="skillId")

    class Config:
        populate_by_name = True


class FormationRequest(BaseModel):
    formation_id: str | None = Field(default=None, alias="formationId")
    name: str = Field(min_length=1, max_length=40)
    formation_type: Literal[0, 1] = Field(alias="formationType")
    pitch_map: dict[str, dict[str, int]] = Field(alias="pitchMap")

    class Config:
        populate_by_name = True


class FormationDeleteRequest(BaseModel):
    formation_id: str = Field(alias="formationId", min_length=1)

    class Config:
        populate_by_name = True


class SkillAdvancementRequest(BaseModel):
    kind: Literal["chosen", "random"]
    category: int | None = None
    skill_id: int | None = Field(default=None, alias="skillId")

    class Config:
        populate_by_name = True


class CharacteristicChoiceRequest(BaseModel):
    roll_id: str = Field(alias="rollId", min_length=1)
    characteristic_id: int = Field(alias="characteristicId")

    class Config:
        populate_by_name = True