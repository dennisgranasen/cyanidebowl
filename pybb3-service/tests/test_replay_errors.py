from app.api.replays import classify_login_error


def test_active_account_skips_without_invalidating_credential():
    status, detail = classify_login_error("Steam login failed: LoggedInElsewhere")

    assert status == 409
    assert detail["code"] == "STEAM_ACCOUNT_ACTIVE"


def test_expired_ticket_requests_renewal():
    status, detail = classify_login_error("InvalidRefreshToken")

    assert status == 401
    assert detail["code"] == "REPLAY_CREDENTIAL_INVALID"


def test_unknown_failure_is_temporary_and_will_be_retried_later():
    status, detail = classify_login_error("Steam is temporarily unreachable")

    assert status == 503
    assert "next scheduled run" in detail["message"]
