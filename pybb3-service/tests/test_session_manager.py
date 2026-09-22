import pytest
from bb3 import SteamAuthState, SteamGuardChallenge
from app.services.session_manager import SessionManager, SessionNotFound

class Flow:
    def __init__(self): self.closed=False
    def start(self, username, _password): return SteamGuardChallenge("email_code", "x***@mail")
    def submit_code(self, _code): return SteamAuthState("steam-user", "secret-refresh")
    def confirm_device(self): return SteamAuthState("steam-user", "secret-refresh")
    def close(self): self.closed=True

class Client:
    def __init__(self): self.closed=False
    def close(self): self.closed=True

def manager(): return SessionManager(lambda: Flow(), lambda _state: (Client(), "7656119"))

def test_challenge_cannot_be_used_by_another_owner():
    service=manager(); challenge=service.start_auth("alice","u","p")
    with pytest.raises(SessionNotFound): service.submit_code("bob",challenge["challengeId"],"12345")

def test_session_cannot_be_used_or_closed_by_another_owner():
    service=manager(); challenge=service.start_auth("alice","u","p")
    result=service.submit_code("alice",challenge["challengeId"],"12345")
    with pytest.raises(SessionNotFound): service.session_info("bob",result["sessionId"])
    with pytest.raises(SessionNotFound): service.close_session("bob",result["sessionId"])
    assert service.session_info("alice",result["sessionId"])["connected"] is True
