from fastapi import FastAPI
from app.config import settings
from app.api import auth, teams

app = FastAPI(
    title="PyBB3 Service API",
    description="Bridge service between Spring Boot backend and Cyanide Blood Bowl 3 TCP Servers.",
    version="1.0.0"
)

# Inkludera routrar
app.include_router(auth.router, prefix=settings.API_V1_STR)
app.include_router(teams.router, prefix=settings.API_V1_STR)

@app.get("/health", tags=["Health"])
def health_check():
    return {"status": "UP", "service": settings.SERVICE_NAME}