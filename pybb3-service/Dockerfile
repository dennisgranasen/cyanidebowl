FROM mcr.microsoft.com/dotnet/sdk:10.0 AS source
ARG PYBB3_REF=main
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/*
WORKDIR /src
RUN git clone https://github.com/dennisgranasen/pybb3.git && cd pybb3 && git checkout "$PYBB3_REF"
RUN dotnet publish /src/pybb3/steam-helper/BB3SteamAuth.csproj -c Release -o /out/helper --self-contained true -p:PublishSingleFile=true

FROM python:3.12-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends libicu-dev ca-certificates \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY --from=source /src/pybb3 /src/pybb3
RUN pip install --no-cache-dir /src/pybb3 && rm -rf /src/pybb3
COPY --from=source /out/helper /app/steam-helper
COPY app ./app
RUN mkdir -p /var/lib/pybb3/credentials && chown -R 65532:65532 /var/lib/pybb3
ENV PYTHONUNBUFFERED=1 PYTHONPATH=/app STEAM_HELPER_PATH=/app/steam-helper/BB3SteamAuth
USER 65532:65532
EXPOSE 8000
CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8000"]
