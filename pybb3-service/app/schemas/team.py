from pydantic import BaseModel, Field

class AddSkillRequest(BaseModel):
    player_id: str = Field(alias="playerId")
    skill_id: int = Field(alias="skillId")

    class Config:
        populate_by_name = True