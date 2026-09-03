import os

class Config:
    SERVICE_NAME: str = "pybb3-service"
    API_V1_STR: str = "/api/v1"
    SESSION_TTL_SECONDS: int = int(os.getenv("SESSION_TTL_SECONDS", "1800")) # 30 minuter standard
    STEAM_HELPER_PATH: str = os.getenv("STEAM_HELPER_PATH", "/app/steam-helper/SteamKitHelper")

settings = Config()