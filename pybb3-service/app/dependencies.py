from fastapi import Header, HTTPException, status
from app.services.session_manager import session_manager
from pybb3.client import BB3Client

def get_bb3_client(x_bb3_session_token: str = Header(..., alias="X-BB3-Session-Token")) -> BB3Client:
    """
    Dependency injection för att hämta aktiv BB3Client baserat på sessionstoken i header.
    """
    return session_manager.get_client(x_bb3_session_token)