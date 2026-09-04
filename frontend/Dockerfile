# Stage 1: Build the React application
FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG REACT_APP_BACKEND_URI
ARG REACT_APP_AUTH0_DOMAIN
ARG REACT_APP_AUTH0_CLIENT_ID
ARG REACT_APP_AUTH0_AUDIENCE

ENV REACT_APP_BACKEND_URI=${REACT_APP_BACKEND_URI}
ENV REACT_APP_AUTH0_DOMAIN=${REACT_APP_AUTH0_DOMAIN}
ENV REACT_APP_AUTH0_CLIENT_ID=${REACT_APP_AUTH0_CLIENT_ID}
ENV REACT_APP_AUTH0_AUDIENCE=${REACT_APP_AUTH0_AUDIENCE}

RUN npm run build

# Stage 2: Serve static files with Nginx
FROM nginx:stable-alpine3.17-slim

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

WORKDIR /usr/share/nginx/html
RUN rm -rf ./*

# Kopierar från public enligt din Webpack-config:
COPY --from=builder /app/public .
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

ENTRYPOINT ["nginx", "-g", "daemon off;"]