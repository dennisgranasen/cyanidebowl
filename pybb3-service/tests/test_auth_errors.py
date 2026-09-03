import pytest
from fastapi import HTTPException

from app.api.auth import auth_failure


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
