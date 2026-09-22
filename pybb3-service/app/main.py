from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.config import settings
from app.api import auth, teams, replays

from app.services.session_manager import session_manager
@asynccontextmanager
async def lifespan(_app):
    yield
    session_manager.close_all()
app = FastAPI(
    title="PyBB3 Service API",
    description="Bridge service between Spring Boot backend and Cyanide Blood Bowl 3 TCP Servers.",
    version="1.0.0", lifespan=lifespan
)

# Inkludera routrar
app.include_router(auth.router, prefix=settings.API_V1_STR)
app.include_router(teams.router, prefix=settings.API_V1_STR)
app.include_router(replays.router, prefix=settings.API_V1_STR)

@app.get("/health", tags=["Health"])
def health_check():
    return {"status": "UP", "service": settings.SERVICE_NAME}
