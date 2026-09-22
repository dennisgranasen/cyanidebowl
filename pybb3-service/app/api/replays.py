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
from app.services.replay_parser import parse_replay,parse_replay_artifact

router=APIRouter(prefix="/replays",tags=["Replays"])

class ReplayBatchRequest(BaseModel):
    credentialId:str='replay-sweeper'
    gameIds:list[str]=Field(min_length=1,max_length=50)

class ReplayAnalysisRequest(BaseModel):
    data:str

class MatchDiscoveryRequest(BaseModel):
    credentialId:str='replay-sweeper'
    leagueId:str
    afterGameId:str|None=None
    pageSize:int=Field(default=50,ge=1,le=100)
    maxPages:int=Field(default=20,ge=1,le=100)

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

def _team_summary(team):
    if team is None:return None
    return {"teamId":team.team_id,"name":team.name,"raceId":team.race_id,"value":team.value}

def _gamer_summary(gamer):
    if gamer is None:return None
    return {"gamerId":gamer.gamer_id,"name":gamer.name}

def _competition_summary(competition):
    if competition is None:return None
    return {"competitionId":competition.competition_id,"name":competition.name,
            "leagueId":competition.league_id,"status":competition.status,
            "day":competition.day,"format":competition.format}

def _game_summary(game):
    return {"gameId":game.game_id,"matchId":game.match_id,
            "homeScore":game.home_score,"awayScore":game.away_score,
            "homeValidation":game.home_validation,"awayValidation":game.away_validation,
            "hasPendingValidation":game.has_pending_validation,
            "homeTeam":_team_summary(game.home_team),"awayTeam":_team_summary(game.away_team),
            "homeGamer":_gamer_summary(game.home_gamer),"awayGamer":_gamer_summary(game.away_gamer),
            "competition":_competition_summary(game.competition)}

@router.post("/discover")
def discover_matches(req:MatchDiscoveryRequest,_owner:str=Depends(trusted_owner)):
    try:stored=credential_store.load(req.credentialId)
    except (KeyError,RuntimeError) as error:raise HTTPException(401,{"code":"REPLAY_CREDENTIAL_MISSING","message":"The replay service Steam ticket must be renewed"}) from error
    state=SteamAuthState(stored['username'],stored['refreshToken'],stored.get('guardData'))
    after=(req.afterGameId or '').strip() or None
    results=[]
    cursor_found=after is None
    try:
        with BB3Client(steam_auth=SteamAuthProcess.from_state(state,helper=settings.STEAM_HELPER_PATH)) as client:
            client.login()
            for page in range(req.maxPages):
                games=client.get_games_model(size=req.pageSize,start=page*req.pageSize,
                                             league_ids=[req.leagueId],is_live=[False],
                                             has_replay=[True],descending=True)
                for game in games.games:
                    if after is not None and game.game_id == after:
                        cursor_found=True
                        break
                    results.append(_game_summary(game))
                if cursor_found and after is not None:break
                if len(games.games) < req.pageSize:break
    except Exception as error:
        status,detail=classify_login_error(str(error))
        raise HTTPException(status,detail) from error
    if after is not None and not cursor_found:
        return {"results":[],"cursorFound":False,"scanned":req.pageSize*req.maxPages}
    return {"results":results,"cursorFound":cursor_found,"scanned":len(results)}

@router.post("/analyze")
def analyze(req:ReplayAnalysisRequest,_owner:str=Depends(trusted_owner)):
    try:
        raw=base64.b64decode(req.data,validate=True)
        if raw[:2]==b'\x1f\x8b': raw=gzip.decompress(raw)
        if raw.startswith(b'PK\x03\x04'):
            original=raw
            original_format,xml,artifacts=parse_replay_artifact(raw)
        elif raw.lstrip().startswith(b'<Replay'):
            xml=raw
            original=encode_bbr(xml)
            original_format='BBR'
            artifacts=parse_replay(xml)
        else:
            original=raw
            xml=decode_replay_data(raw.decode('ascii').strip())
            original_format='BBR'
            artifacts=parse_replay(xml)
        return {"compactData":base64.b64encode(artifacts["compactGzip"]).decode("ascii"),
                "originalData":base64.b64encode(original).decode("ascii"),
                "originalSha256":hashlib.sha256(original).hexdigest(),"compactSha256":artifacts["compactSha256"],
                "originalFormat":original_format,
                "analysis":artifacts["analysis"]}
    except Exception as error:
        raise HTTPException(400,{"code":"INVALID_REPLAY","message":"Replay could not be parsed"}) from error

@router.get("/credentials/{credential_id}")
def credential_status(credential_id:str,_owner:str=Depends(trusted_owner)):
    return credential_store.status(credential_id)
