# ==========================================
# Steg 1: Klona pybb3-repot och bygg Steam-hjälparen (.NET)
# ==========================================
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS dotnet-builder
ARG GITHUB_TOKEN

# Installera git i builder-stegen
RUN apt-get update && apt-get install -y git

WORKDIR /src

# Klona pybb3-repot till /src/pybb3_repo
RUN git clone https://${GITHUB_TOKEN}@github.com/DITT_ANVÄNDARNAMN/pybb3.git pybb3_repo

# Gå in i dotnet-mappen enligt din struktur (./steam-helper)
WORKDIR /src/pybb3_repo/steam-helper

# Återställ beroenden och publicera binären
RUN dotnet restore
RUN dotnet publish -c Release -o /app/dotnet_out

# ==========================================
# Steg 2: Bygg Python-miljön (FastAPI + PyBB3)
# ==========================================
FROM python:3.11-slim AS runner
ARG GITHUB_TOKEN

# Installera git (krävs för pip install git+https) och .NET Runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    git \
    ca-certificates \
    wget \
    && wget https://packages.microsoft.com/config/debian/12/packages-microsoft-prod.deb -O packages-microsoft-prod.deb \
    && dpkg -i packages-microsoft-prod.deb \
    && rm packages-microsoft-prod.deb \
    && apt-get update \
    && apt-get install -y --no-install-recommends dotnet-runtime-8.0 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Kopiera kravfilen och ersätt variabeln ${GITHUB_TOKEN} vid installation
COPY requirements.txt .
RUN eval "echo \"$(cat requirements.txt)\"" > requirements.processed.txt \
    && pip install --no-cache-dir -r requirements.processed.txt \
    && rm requirements.processed.txt

# Kopiera den färdigbyggda .NET-binären från Steg 1
COPY --from=dotnet-builder /app/dotnet_out ./steam-helper

# Kopiera endast app-koden för själva mikrotjänsten (pybb3 ligger nu i site-packages)
COPY app ./app

ENV PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]