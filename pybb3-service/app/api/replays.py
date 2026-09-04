import base64
from fastapi import APIRouter,Depends,HTTPException
from pydantic import BaseModel,Field
from bb3 import BB3Client,SteamAuthProcess,SteamAuthState
from app.config import settings
from app.dependencies import trusted_owner
from app.services.credential_store import credential_store

router=APIRouter(prefix="/replays",tags=["Replays"])

class ReplayBatchRequest(BaseModel):
    credentialId:str='replay-sweeper'
    gameIds:list[str]=Field(min_length=1,max_length=50)

def classify_login_error(message:str):
    if any(token in message for token in ('LoggedInElsewhere','AlreadyLogged','PlayingElsewhere','account is active','AccountInUse','LogonSessionReplaced')):
        return 409,{"code":"STEAM_ACCOUNT_ACTIVE","message":"Steam account is currently active; replay download was skipped"}
    if any(token in message for token in ('InvalidRefreshToken','InvalidPassword','AccessDenied','Expired')):
        return 401,{"code":"REPLAY_CREDENTIAL_INVALID","message":"The replay service Steam ticket must be renewed"}
    return 503,{"code":"STEAM_TEMPORARILY_UNAVAILABLE","message":"Steam login failed temporarily; replay download will be retried at the next scheduled run"}

@router.post("/batch")
def download_batch(req:ReplayBatchRequest,_owner:str=Depends(trusted_owner)):
    try: stored=credential_store.load(req.credentialId)
    except (KeyError,RuntimeError) as error: raise HTTPException(401,{"code":"REPLAY_CREDENTIAL_MISSING","message":"The replay service Steam ticket must be renewed"}) from error
    state=SteamAuthState(stored['username'],stored['refreshToken'],stored.get('guardData'))
    results=[]
    try:
        with BB3Client(steam_auth=SteamAuthProcess.from_state(state,helper=settings.STEAM_HELPER_PATH)) as client:
            client.login()
            for game_id in req.gameIds:
                try: results.append({"gameId":game_id,"data":base64.b64encode(client.download_replay(game_id)).decode("ascii")})
                except Exception: results.append({"gameId":game_id,"error":"Replay unavailable"})
    except Exception as error:
        status,detail=classify_login_error(str(error))
        raise HTTPException(status,detail) from error
    return {"results":results}

@router.get("/credentials/{credential_id}")
def credential_status(credential_id:str,_owner:str=Depends(trusted_owner)):
    return credential_store.status(credential_id)
