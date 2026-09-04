# Stage 1: Build the React application
FROM node:18-alpine AS builder

WORKDIR /app

# Copy dependency definitions
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy application source code
COPY . .

# Pass build-time environment variables to React
ARG REACT_APP_BACKEND_URI
ARG REACT_APP_AUTH0_DOMAIN
ARG REACT_APP_AUTH0_CLIENT_ID
ARG REACT_APP_AUTH0_AUDIENCE

ENV REACT_APP_BACKEND_URI=${REACT_APP_BACKEND_URI}
ENV REACT_APP_AUTH0_DOMAIN=${REACT_APP_AUTH0_DOMAIN}
ENV REACT_APP_AUTH0_CLIENT_ID=${REACT_APP_AUTH0_CLIENT_ID}
ENV REACT_APP_AUTH0_AUDIENCE=${REACT_APP_AUTH0_AUDIENCE}

# Build static production assets into /app/build
RUN npm run build

# Stage 2: Serve static files with Nginx
FROM nginx:stable-alpine3.17-slim

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

WORKDIR /usr/share/nginx/html

# Clean default Nginx files
RUN rm -rf ./*

# Copy compiled build output from Stage 1
COPY --from=builder /app/public .

EXPOSE 80

ENTRYPOINT ["nginx", "-g", "daemon off;"]