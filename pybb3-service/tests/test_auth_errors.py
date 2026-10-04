import pytest
from fastapi import HTTPException

from app.api import auth as auth_api
from app.api.auth import auth_failure
from app.schemas.auth import SteamLoginRequest


def test_invalid_password_is_a_safe_client_error():
    with pytest.raises(HTTPException) as caught:
        auth_failure(RuntimeError("Authentication failed with result InvalidPassword"))

    assert caught.value.status_code == 400
    assert caught.value.detail == {
        "code": "INVALID_STEAM_CREDENTIALS",
        "message": "Steam username or password is incorrect",
    }


def test_unknown_helper_failure_does_not_expose_diagnostic():
    with pytest.raises(HTTPException) as caught:
        auth_failure(RuntimeError("secret internal diagnostic"))

    assert caught.value.status_code == 502
    assert "secret" not in str(caught.value.detail)


def test_auth_persists_credential_only_when_requested_and_keeps_session(monkeypatch):
    credential = {"username": "steam-user", "refreshToken": "secret-refresh", "guardData": None}
    stored = []
    monkeypatch.setattr(auth_api.session_manager, "start_auth", lambda *_args: {
        "status": "AUTHENTICATED", "sessionId": "session-1", "steamUsername": "steam-user",
        "steamId": "7656119", "credential": credential,
    })
    monkeypatch.setattr(auth_api.credential_store, "save", lambda key, value: stored.append((key, value)))
    monkeypatch.setattr(auth_api.credential_store, "owner_credential_id", lambda owner: f"id-{owner}")
    monkeypatch.setattr(auth_api.credential_store, "configured", lambda: True)

    result = auth_api.start(SteamLoginRequest(
        username="steam-user", password="not-stored", persistCredential=True,
    ), owner="owner-1")

    assert result["sessionId"] == "session-1"
    assert "credential" not in result
    assert stored == [("id-owner-1", credential)]


def test_successful_login_without_opt_in_forgets_previous_credential(monkeypatch):
    deleted = []
    monkeypatch.setattr(auth_api.credential_store, "owner_credential_id", lambda owner: f"id-{owner}")
    monkeypatch.setattr(auth_api.credential_store, "delete", lambda key: deleted.append(key))

    result = auth_api.persist({"status": "AUTHENTICATED"}, "owner-1", False)

    assert result == {"status": "AUTHENTICATED"}
    assert deleted == ["id-owner-1"]


def test_remember_login_is_rejected_when_encryption_is_not_configured(monkeypatch):
    monkeypatch.setattr(auth_api.credential_store, "configured", lambda: False)
    monkeypatch.setattr(auth_api.session_manager, "start_auth", lambda *_args: pytest.fail("Steam login must not start"))

    with pytest.raises(HTTPException) as caught:
        auth_api.start(SteamLoginRequest(
            username="steam-user", password="password", persistCredential=True,
        ), owner="owner-1")

    assert caught.value.status_code == 503
    assert caught.value.detail["code"] == "STEAM_REMEMBER_UNAVAILABLE"


def test_restore_auth_returns_new_session_without_returning_credential(monkeypatch):
    monkeypatch.setattr(auth_api.credential_store, "configured", lambda: True)
    monkeypatch.setattr(auth_api.credential_store, "owner_credential_id", lambda owner: f"id-{owner}")
    monkeypatch.setattr(auth_api.credential_store, "load", lambda _key: {
        "username": "steam-user", "refreshToken": "secret-refresh", "guardData": "secret-guard",
    })
    monkeypatch.setattr(auth_api.session_manager, "restore_auth", lambda owner, _credential: {
        "status": "AUTHENTICATED", "sessionId": "session-2", "steamUsername": "steam-user",
        "steamId": "7656119",
    })

    result = auth_api.restore(owner="owner-1")

    assert result["sessionId"] == "session-2"
    assert result["remembered"] is True
    assert "refreshToken" not in result
