from app.config import settings
from app.services.credential_store import CredentialStore


def test_owner_credentials_are_encrypted_and_can_be_forgotten(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "CREDENTIAL_ENCRYPTION_KEY", "test-encryption-key")
    monkeypatch.setattr(settings, "CREDENTIAL_DIRECTORY", str(tmp_path))
    store = CredentialStore()
    credential_id = store.owner_credential_id("auth0|user-1")

    store.save(credential_id, {
        "username": "steam-user",
        "refreshToken": "secret-refresh-token",
        "guardData": "secret-guard-data",
    })

    stored_files = list(tmp_path.glob("*.credential"))
    assert credential_id.startswith("user-")
    assert credential_id != store.owner_credential_id("auth0|user-2")
    assert len(stored_files) == 1
    assert b"secret-refresh-token" not in stored_files[0].read_bytes()
    assert store.load(credential_id)["refreshToken"] == "secret-refresh-token"
    assert store.delete(credential_id) is True
    assert store.delete(credential_id) is False