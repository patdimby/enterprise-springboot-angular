# ---------- Build : compilation Angular ----------
# Étape 1 : Node compile l'application (`ng build` sort dans dist/frontend).
FROM node:22-alpine AS build
WORKDIR /workspace

# Cache : d'abord les dépendances (package*.json), puis le code.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# `ng build` (configuration production par défaut) → dist/frontend/browser
RUN npm run build

# ---------- Runtime : nginx sert les fichiers statiques ----------
# Étape 2 : image minuscule, avec le proxy /api vers le backend.
FROM nginx:1.27-alpine

# Configuration nginx : SPA fallback + proxy /api → backend:8080.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Les fichiers compilés sont copiés dans la racine web de nginx.
COPY --from=build /workspace/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost/ >/dev/null || exit 1
