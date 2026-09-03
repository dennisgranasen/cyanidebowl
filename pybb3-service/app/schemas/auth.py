from pydantic import BaseModel, Field
class SteamLoginRequest(BaseModel):
    username: str=Field(min_length=1,max_length=128)
    password: str=Field(min_length=1,max_length=512)
class GuardCodeRequest(BaseModel):
    code: str=Field(min_length=1,max_length=32)
