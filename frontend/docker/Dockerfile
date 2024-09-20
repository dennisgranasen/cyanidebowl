FROM nginx:stable-alpine3.17-slim

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

HEALTHCHECK --interval=2m --timeout=15s --start-period=45s \
    CMD curl --retry 6 --max-time 5 --retry-delay 10 --retry-max-time 60 \
             --fail --silent --show-error http://localhost/version.json \
          || bash -c 'kill --signal SIGTERM -1 && (sleep 10; kill --signal SIGKILL -1)'

WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY ./public/ .
ENTRYPOINT ["nginx", "-g", "daemon off;"]




