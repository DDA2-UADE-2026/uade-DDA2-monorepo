FROM node:24-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts

COPY . .

ARG VITE_SERVER_URL
ARG VITE_CLIENT_URL

ENV VITE_SERVER_URL=${VITE_SERVER_URL}
ENV VITE_CLIENT_URL=${VITE_CLIENT_URL}

RUN npm run build

FROM nginx/nginx-unprivileged:stable-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

USER 101:101

EXPOSE 8080
