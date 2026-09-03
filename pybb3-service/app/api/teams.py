from fastapi import APIRouter,Depends,HTTPException,Query
from app.dependencies import trusted_owner
from app.services.session_manager import SessionNotFound,session_manager
from app.services.team_mapper import teams_response
router=APIRouter(prefix="/sessions",tags=["Teams"])
@router.get("/{session_id}/teams")
def my_teams(session_id:str,owner:str=Depends(trusted_owner),size:int=Query(50,ge=1,le=100),start:int=Query(0,ge=0)):
    try:
        root=session_manager.call(owner,session_id,lambda client:client.get_teams_of_gamer(size=size,start=start))
        return teams_response(root,start=start,size=size)
    except SessionNotFound as error:raise HTTPException(404,str(error)) from error
    except (ValueError,RuntimeError,OSError) as error:raise HTTPException(502,"Unable to retrieve BB3 teams") from error
@router.get("/{session_id}/teams/{team_id}/roster")
def roster(session_id:str,team_id:str,owner:str=Depends(trusted_owner)):
    try:return session_manager.call(owner,session_id,lambda client:client.get_team_roster_model(team_id))
    except SessionNotFound as error:raise HTTPException(404,str(error)) from error
