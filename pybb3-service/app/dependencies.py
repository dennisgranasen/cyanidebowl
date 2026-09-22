import hmac
from fastapi import Header, HTTPException
from app.config import settings
def trusted_owner(x_internal_api_key: str=Header(...,alias="X-Internal-Api-Key"), x_owner_id: str=Header(...,alias="X-Owner-Id")):
    if not settings.INTERNAL_API_KEY or not hmac.compare_digest(x_internal_api_key,settings.INTERNAL_API_KEY):
        raise HTTPException(401,"Invalid service credentials")
    if not x_owner_id.strip(): raise HTTPException(400,"Missing owner")
    return x_owner_id
