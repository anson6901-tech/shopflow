# Stage 1: build the React app
FROM node:20-alpine AS build
WORKDIR /build
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: serve the built files with nginx
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 8080
