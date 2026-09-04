import base64
import gzip
import hashlib
import zlib
from fastapi import APIRouter,Depends,HTTPException
from pydantic import BaseModel,Field
from bb3 import BB3Client,SteamAuthProcess,SteamAuthState
from bb3.encoding import b64_encode_text
from bb3.replay import decode_replay_data
from app.config import settings
from app.dependencies import trusted_owner
from app.services.credential_store import credential_store
from app.services.replay_parser import parse_replay

router=APIRouter(prefix="/replays",tags=["Replays"])

class ReplayBatchRequest(BaseModel):
    credentialId:str='replay-sweeper'
    gameIds:list[str]=Field(min_length=1,max_length=50)

class ReplayAnalysisRequest(BaseModel):
    data:str

def download_replay_artifact(client:BB3Client,game_id:str)->tuple[bytes,bytes]:
    root=client.request('RequestDownloadReplay','ResponseDownloadReplay',f'<GameId>{b64_encode_text(game_id)}</GameId>')
    replay_data=root.findtext('ReplayData')
    if not replay_data: raise RuntimeError('Replay response contained no ReplayData')
    bbr=replay_data.encode('ascii')
    return bbr,decode_replay_data(replay_data)

def encode_bbr(xml:bytes)->bytes:
    return base64.b64encode(base64.b64encode(zlib.compress(xml)))

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
                try:
                    bbr,xml=download_replay_artifact(client,game_id)
                    artifacts=parse_replay(xml)
                    results.append({"gameId":game_id,"data":base64.b64encode(bbr).decode("ascii"),
                                    "compactData":base64.b64encode(artifacts["compactGzip"]).decode("ascii"),
                                    "originalSha256":hashlib.sha256(bbr).hexdigest(),"compactSha256":artifacts["compactSha256"],
                                    "originalFormat":"BBR",
                                    "analysis":artifacts["analysis"]})
                except Exception: results.append({"gameId":game_id,"error":"Replay unavailable"})
    except Exception as error:
        status,detail=classify_login_error(str(error))
        raise HTTPException(status,detail) from error
    return {"results":results}

@router.post("/analyze")
def analyze(req:ReplayAnalysisRequest,_owner:str=Depends(trusted_owner)):
    try:
        raw=base64.b64decode(req.data,validate=True)
        if raw[:2]==b'\x1f\x8b': raw=gzip.decompress(raw)
        if raw.lstrip().startswith(b'<Replay'):
            xml=raw
            bbr=encode_bbr(xml)
        else:
            bbr=raw
            xml=decode_replay_data(raw.decode('ascii').strip())
        artifacts=parse_replay(xml)
        return {"compactData":base64.b64encode(artifacts["compactGzip"]).decode("ascii"),
                "originalData":base64.b64encode(bbr).decode("ascii"),
                "originalSha256":hashlib.sha256(bbr).hexdigest(),"compactSha256":artifacts["compactSha256"],
                "originalFormat":"BBR",
                "analysis":artifacts["analysis"]}
    except Exception as error:
        raise HTTPException(400,{"code":"INVALID_REPLAY","message":"Replay could not be parsed"}) from error

@router.get("/credentials/{credential_id}")
def credential_status(credential_id:str,_owner:str=Depends(trusted_owner)):
    return credential_store.status(credential_id)
