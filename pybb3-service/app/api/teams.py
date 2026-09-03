from fastapi import APIRouter,Depends,HTTPException
from app.dependencies import trusted_owner
from app.services.session_manager import SessionNotFound,session_manager
router=APIRouter(prefix="/sessions",tags=["Teams"])
@router.get("/{session_id}/teams/{team_id}/roster")
def roster(session_id:str,team_id:str,owner:str=Depends(trusted_owner)):
    try:return session_manager.call(owner,session_id,lambda client:client.get_team_roster_model(team_id))
    except SessionNotFound as error:raise HTTPException(404,str(error)) from error
