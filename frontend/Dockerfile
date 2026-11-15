FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_BACKEND_URL=ws://localhost:6593/ws
RUN npm test && npm run lint && npm run build

FROM nginx:stable-alpine
COPY --from=build /app/dist/ /usr/share/nginx/html/
EXPOSE 80
