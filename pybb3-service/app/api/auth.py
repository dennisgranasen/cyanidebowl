from fastapi import APIRouter, Header, Response, status
from app.schemas.auth import SteamLoginRequest, LoginResponse
from app.services.session_manager import session_manager

router = APIRouter(prefix="/auth", tags=["Auth"])

@router.post("/steam-login", response_model=LoginResponse)
def steam_login(req: SteamLoginRequest):
    token = session_manager.create_session(
        username=req.username,
        password=req.password,
        two_factor=req.two_factor_code
    )
    return LoginResponse(
        status="SUCCESS",
        bb3SessionToken=token,
        message="Logged in successfully to BB3 TCP server"
    )

@router.post("/logout")
def logout(x_bb3_session_token: str = Header(..., alias="X-BB3-Session-Token")):
    session_manager.close_session(x_bb3_session_token)
    return Response(status_code=status.HTTP_200_OK)