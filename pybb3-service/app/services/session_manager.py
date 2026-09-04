from __future__ import annotations

from dataclasses import dataclass, field
import threading
import time
import uuid
import base64
import binascii
from bb3 import BB3Client, SteamAuthProcess, SteamAuthState, SteamGuardChallenge, SteamWebAuthFlow
from app.config import settings

class SessionNotFound(Exception): pass

@dataclass
class PendingAuth:
    owner_id: str
    flow: SteamWebAuthFlow
    persist_credential: bool = False
    last_access: float = field(default_factory=time.time)

@dataclass
class ActiveSession:
    owner_id: str
    username: str
    steam_id: str
    client: BB3Client
    coach_id: str | None = None
    coach_name: str | None = None
    lock: threading.RLock = field(default_factory=threading.RLock)
    last_access: float = field(default_factory=time.time)

class SessionManager:
    def __init__(self, flow_factory=None, client_factory=None):
        self._pending, self._sessions = {}, {}
        self._lock = threading.RLock()
        self._flow_factory = flow_factory or (lambda: SteamWebAuthFlow(helper=settings.STEAM_HELPER_PATH))
        self._client_factory = client_factory or self._open_client

    def start_auth(self, owner_id, username, password, persist_credential=False):
        self.cleanup_expired()
        flow = self._flow_factory()
        try: result = flow.start(username, password)
        except Exception:
            flow.close(); raise
        return self._handle(owner_id, flow, result, persist_credential=persist_credential)

    def submit_code(self, owner_id, challenge_id, code):
        pending = self._get_pending(owner_id, challenge_id)
        return self._handle(owner_id, pending.flow, pending.flow.submit_code(code), challenge_id,
                            persist_credential=pending.persist_credential)

    def confirm_device(self, owner_id, challenge_id):
        pending = self._get_pending(owner_id, challenge_id)
        return self._handle(owner_id, pending.flow, pending.flow.confirm_device(), challenge_id,
                            persist_credential=pending.persist_credential)

    def session_info(self, owner_id, session_id):
        session = self._get_session(owner_id, session_id)
        return {"connected": True, "steamUsername": session.username, "steamId": session.steam_id,
                "coachId": session.coach_id, "coachName": session.coach_name}

    def close_session(self, owner_id, session_id):
        with self._lock:
            session = self._sessions.get(session_id)
            if session is None or session.owner_id != owner_id: raise SessionNotFound("Unknown or expired session")
            self._sessions.pop(session_id)
        session.client.close()

    def call(self, owner_id, session_id, operation):
        session = self._get_session(owner_id, session_id)
        with session.lock:
            session.last_access = time.time()
            return operation(session.client)

    def coach_info(self, owner_id, session_id):
        session = self._get_session(owner_id, session_id)
        return session.coach_id, session.coach_name

    def cleanup_expired(self):
        now = time.time()
        with self._lock:
            pending = [self._pending.pop(k) for k, v in list(self._pending.items()) if now-v.last_access > settings.CHALLENGE_TTL_SECONDS]
            sessions = [self._sessions.pop(k) for k, v in list(self._sessions.items()) if now-v.last_access > settings.SESSION_TTL_SECONDS]
        for value in pending: value.flow.close()
        for value in sessions: value.client.close()

    def close_all(self):
        with self._lock:
            pending, sessions = list(self._pending.values()), list(self._sessions.values())
            self._pending.clear(); self._sessions.clear()
        for value in pending: value.flow.close()
        for value in sessions: value.client.close()

    def _handle(self, owner_id, flow, result, challenge_id=None, persist_credential=False):
        if isinstance(result, SteamGuardChallenge):
            challenge_id = challenge_id or str(uuid.uuid4())
            with self._lock: self._pending[challenge_id] = PendingAuth(owner_id, flow, persist_credential)
            return {"status":"GUARD_REQUIRED", "challengeId":challenge_id, "method":result.method,
                    "emailHint":result.email, "previousCodeWasIncorrect":result.previous_code_was_incorrect}
        if challenge_id:
            with self._lock: self._pending.pop(challenge_id, None)
        opened = self._client_factory(result)
        client, steam_id = opened[0], opened[1]
        coach_id, coach_name = (opened[2], opened[3]) if len(opened) >= 4 else (None, None)
        session_id = str(uuid.uuid4())
        with self._lock: self._sessions[session_id] = ActiveSession(owner_id, result.username, steam_id, client, coach_id, coach_name)
        response={"status":"AUTHENTICATED", "sessionId":session_id, "steamUsername":result.username, "steamId":steam_id}
        if persist_credential:
            response["credential"]={"username":result.username,"refreshToken":result.refresh_token,"guardData":result.guard_data}
        return response

    def _open_client(self, state: SteamAuthState):
        client = BB3Client(steam_auth=SteamAuthProcess.from_state(state, helper=settings.STEAM_HELPER_PATH))
        try:
            client.__enter__(); login = client.login()
            return client, client._steam_ticket.steam_id, self._login_value(login, "GamerId", "IdGamer", "Id"), self._login_value(login, "GamerName", "Name")
        except Exception:
            client.close(); raise

    @staticmethod
    def _login_value(root, *names):
        for name in names:
            value = root.findtext(name) or root.findtext(f"Gamer/{name}")
            if value:
                try: return base64.b64decode(value, validate=True).decode("utf-8")
                except (binascii.Error, UnicodeDecodeError): return value
        return None

    def _get_pending(self, owner_id, challenge_id):
        self.cleanup_expired()
        with self._lock:
            value = self._pending.get(challenge_id)
            if value is None or value.owner_id != owner_id: raise SessionNotFound("Unknown or expired challenge")
            value.last_access = time.time(); return value

    def _get_session(self, owner_id, session_id):
        self.cleanup_expired()
        with self._lock:
            value = self._sessions.get(session_id)
            if value is None or value.owner_id != owner_id: raise SessionNotFound("Unknown or expired session")
            value.last_access = time.time(); return value

session_manager = SessionManager()
