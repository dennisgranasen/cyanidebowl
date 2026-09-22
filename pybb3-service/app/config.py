import os
class Config:
    SERVICE_NAME="pybb3-service"; API_V1_STR="/api/v1"
    SESSION_TTL_SECONDS=int(os.getenv("SESSION_TTL_SECONDS","1800"))
    CHALLENGE_TTL_SECONDS=int(os.getenv("CHALLENGE_TTL_SECONDS","300"))
    STEAM_HELPER_PATH=os.getenv("STEAM_HELPER_PATH","/app/steam-helper/BB3SteamAuth")
    INTERNAL_API_KEY=os.getenv("PYBB3_INTERNAL_API_KEY","")
    CREDENTIAL_DIRECTORY=os.getenv("PYBB3_CREDENTIAL_DIRECTORY","/var/lib/pybb3/credentials")
    CREDENTIAL_ENCRYPTION_KEY=os.getenv("PYBB3_CREDENTIAL_ENCRYPTION_KEY","")
settings=Config()
