FROM node:22.12-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG ANGULAR_CONFIGURATION=production
RUN npx ng build web --configuration "$ANGULAR_CONFIGURATION"


FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/web/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]