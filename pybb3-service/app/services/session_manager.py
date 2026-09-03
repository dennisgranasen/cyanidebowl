import uuid
import time
from typing import Dict, Tuple
from fastapi import HTTPException, status
from pybb3.client import BB3Client
from app.config import settings

class SessionManager:
    def __init__(self):
        # key: session_token, value: (BB3Client, last_accessed_timestamp)
        self._sessions: Dict[str, Tuple[BB3Client, float]] = {}

    def create_session(self, username: str, password: str, two_factor: str | None) -> str:
        try:
            # Skapa klient med anpassad sökväg till .NET SteamKit-hjälparen
            client = BB3Client.from_steam(executable_path=settings.STEAM_HELPER_PATH)
            client._steam_auth.username = username
            client._steam_auth.password = password
            client._steam_auth.two_factor = two_factor
            
            # Startar process, ansluter TCP och loggar in mot Cyanide
            client.__enter__()
            client.login()

            token = str(uuid.uuid4())
            self._sessions[token] = (client, time.time())
            return token
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"BB3 Authentication failed: {str(e)}"
            )

    def get_client(self, token: str) -> BB3Client:
        self.cleanup_expired()
        if token not in self._sessions:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired BB3 session token"
            )
        client, _ = self._sessions[token]
        self._sessions[token] = (client, time.time())
        return client

    def close_session(self, token: str):
        if token in self._sessions:
            client, _ = self._sessions.pop(token)
            try:
                client.close()
            except Exception:
                pass

    def cleanup_expired(self):
        now = time.time()
        expired = [
            token for token, (_, last_seen) in self._sessions.items()
            if now - last_seen > settings.SESSION_TTL_SECONDS
        ]
        for token in expired:
            self.close_session(token)

session_manager = SessionManager()