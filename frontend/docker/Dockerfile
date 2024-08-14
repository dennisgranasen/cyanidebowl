FROM nginx:stable-alpine3.17-slim

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

HEALTHCHECK --interval=60s --timeout=5s CMD curl -sSf http://localhost/version.json || exit 1

WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY ./public/ .
ENTRYPOINT ["nginx", "-g", "daemon off;"]




