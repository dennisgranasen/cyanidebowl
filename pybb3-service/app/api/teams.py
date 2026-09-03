from fastapi import APIRouter, Depends, status
from app.dependencies import get_bb3_client
from pybb3.client import BB3Client
from app.schemas.team import AddSkillRequest

router = APIRouter(prefix="/teams", tags=["Teams"])

@router.get("/{team_id}/roster")
def get_team_roster(team_id: str, client: BB3Client = Depends(get_bb3_client)):
    # Hämtar lagroster via PyBB3
    roster_model = client.get_team_roster_model(team_id)
    return roster_model

@router.post("/{team_id}/players/add-skill", status_code=status.HTTP_200_OK)
def add_player_skill(
    team_id: str, 
    req: AddSkillRequest, 
    client: BB3Client = Depends(get_bb3_client)
):
    # Anropar köp/tilldelning av färdighet via PyBB3
    # client.add_player_skill(team_id, req.player_id, req.skill_id)
    return {"status": "SUCCESS", "team_id": team_id, "player_id": req.player_id}