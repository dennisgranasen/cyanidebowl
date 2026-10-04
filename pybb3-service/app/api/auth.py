import logging
from fastapi import APIRouter, Depends, HTTPException, Response
from app.dependencies import trusted_owner
from app.schemas.auth import GuardCodeRequest, SteamLoginRequest
from app.services.session_manager import SessionNotFound, session_manager
from app.services.credential_store import credential_store
router=APIRouter(prefix="/auth",tags=["Auth"])
log=logging.getLogger(__name__)
def missing(error): raise HTTPException(404,str(error)) from error
def auth_failure(error):
    message=str(error)
    if "InvalidPassword" in message:
        log.info("Steam rejected credentials")
        raise HTTPException(400,{"code":"INVALID_STEAM_CREDENTIALS","message":"Steam username or password is incorrect"}) from error
    if "RateLimitExceeded" in message:
        log.warning("Steam authentication rate limit reached")
        raise HTTPException(429,{"code":"STEAM_RATE_LIMITED","message":"Too many Steam login attempts; wait before trying again"}) from error
    if "TwoFactorCodeMismatch" in message or "InvalidLoginAuthCode" in message:
        log.info("Steam rejected a Guard code")
        raise HTTPException(400,{"code":"INVALID_STEAM_GUARD_CODE","message":"The Steam Guard code is incorrect or expired"}) from error
    log.exception("Steam authentication helper failed")
    raise HTTPException(502,{"code":"STEAM_SERVICE_FAILED","message":"Steam authentication service failed"}) from error
@router.post("/start")
def start(req:SteamLoginRequest,owner:str=Depends(trusted_owner)):
    if req.persistCredential and not credential_store.configured():
        raise HTTPException(503,{"code":"STEAM_REMEMBER_UNAVAILABLE","message":"Encrypted Steam credential storage is not configured"})
    try:return persist(session_manager.start_auth(owner,req.username,req.password,req.persistCredential),owner,req.persistCredential)
    except (ValueError,RuntimeError,OSError) as error:auth_failure(error)
@router.post("/challenges/{challenge_id}/code")
def code(challenge_id:str,req:GuardCodeRequest,owner:str=Depends(trusted_owner)):
    try:return persist(session_manager.submit_code(owner,challenge_id,req.code),owner,True)
    except SessionNotFound as error:missing(error)
    except (ValueError,RuntimeError,OSError) as error:auth_failure(error)
@router.post("/challenges/{challenge_id}/confirm")
def confirm(challenge_id:str,owner:str=Depends(trusted_owner)):
    try:return persist(session_manager.confirm_device(owner,challenge_id),owner,True)
    except SessionNotFound as error:missing(error)
    except (ValueError,RuntimeError,OSError) as error:auth_failure(error)
@router.get("/remembered")
def remembered(owner:str=Depends(trusted_owner)):
    credential_id=credential_store.owner_credential_id(owner)
    return {"rememberAvailable":credential_store.configured(),"remembered":credential_store.exists(credential_id)}
@router.delete("/remembered")
def forget(owner:str=Depends(trusted_owner)):
    return {"deleted":credential_store.delete(credential_store.owner_credential_id(owner))}
@router.post("/restore")
def restore(owner:str=Depends(trusted_owner)):
    if not credential_store.configured():
        raise HTTPException(503,{"code":"STEAM_REMEMBER_UNAVAILABLE","message":"Encrypted Steam credential storage is not configured"})
    try:
        credential=credential_store.load(credential_store.owner_credential_id(owner))
    except KeyError as error:
        raise HTTPException(404,"No remembered Steam session") from error
    try:
        result=session_manager.restore_auth(owner,credential)
        result["remembered"]=True
        return result
    except (ValueError,RuntimeError,OSError) as error:
        log.warning("Remembered Steam session could not be restored")
        raise HTTPException(401,{"code":"STEAM_RECONNECT_REQUIRED","message":"Reconnect Steam to continue"}) from error
@router.get("/sessions/{session_id}")
def session(session_id:str,owner:str=Depends(trusted_owner)):
    try:return session_manager.session_info(owner,session_id)
    except SessionNotFound as error:missing(error)
@router.delete("/sessions/{session_id}",status_code=204)
def logout(session_id:str,owner:str=Depends(trusted_owner)):
    try:session_manager.close_session(owner,session_id)
    except SessionNotFound as error:missing(error)
    return Response(status_code=204)
def persist(result,owner,requested):
    credential=result.pop('credential',None)
    if requested and credential:
        credential_store.save(credential_store.owner_credential_id(owner),credential)
    elif result.get('status')=='AUTHENTICATED':
        credential_store.delete(credential_store.owner_credential_id(owner))
    return result
