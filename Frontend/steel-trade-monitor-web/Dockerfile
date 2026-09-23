# Etapa 1: build do Angular
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# Etapa 2: nginx servindo os estáticos e fazendo proxy do /api
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/steel-trade-monitor-web/browser /usr/share/nginx/html
EXPOSE 80
