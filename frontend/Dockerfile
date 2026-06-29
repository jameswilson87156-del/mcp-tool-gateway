FROM node:20-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE=http://localhost:8080/api
RUN VITE_API_BASE=${VITE_API_BASE} npm run build

FROM nginx:1.28-alpine

COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
