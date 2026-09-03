from pydantic import BaseModel, Field
from typing import Optional

class SteamLoginRequest(BaseModel):
    username: str
    password: str
    two_factor_code: Optional[str] = Field(default=None, alias="twoFactorCode")

    class Config:
        populate_by_name = True

class LoginResponse(BaseModel):
    status: str
    bb3_session_token: str = Field(alias="bb3SessionToken")
    message: Optional[str] = None

    class Config:
        populate_by_name = True