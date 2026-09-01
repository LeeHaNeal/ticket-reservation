# ---- 1단계: 빌드 ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# 배포 환경에서는 nginx가 같은 오리진에서 /api를 백엔드로 프록시해주므로
# baseURL을 비워서 상대 경로(/api/...)로 요청하도록 한다.
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---- 2단계: 정적 파일 서빙 ----
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
