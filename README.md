# enterprise-springboot-angular

Application d'entreprise **gestion de projets** : API REST **Spring Boot 4.1** (Java 21,
Spring Security 7 + JWT, MySQL 8, Flyway, Swagger) et frontend **Angular 20 + NgRx**
(en phase 2). Orchestration **Docker Compose**, CI **GitHub Actions**.

> 📐 Architecture détaillée : [ARCHITECTURE.md](ARCHITECTURE.md)

## Démarrage rapide (dev local)

```bash
# 1. Base de données
docker compose up -d mysql

# 2. Backend (Maven Wrapper — aucune installation requise)
cd backend
./mvnw spring-boot:run          # Windows : mvnw.cmd spring-boot:run
```

- API : http://localhost:8080/api
- Swagger UI : http://localhost:8080/swagger-ui.html
- Admin seedé au 1er démarrage : `admin@enterprise.com` / `Admin123!`

## Essayer l'API

```bash
# Connexion
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@enterprise.com","password":"Admin123!"}'

# Appel authentifié (copier le token de la réponse)
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer <TOKEN>"
```

## Docker (stack complète)

```bash
cp .env.example .env    # adapter les mots de passe + secret JWT
docker compose up --build
```

## Tests

```bash
cd backend
./mvnw test             # H2 en mémoire, profil "test"
```

## Structure

```
backend/    API Spring Boot 4.1 (auth JWT, users, rôles)   ✔ phase 1
frontend/   Angular 20 + NgRx                              ⏳ phase 2
```

## Roadmap

- [x] **Phase 1** — Squelette backend : sécurité JWT, auth, users, erreurs RFC 7807, Docker, CI
- [ ] **Phase 2** — Entités métier (Project, Task, Comment) + migrations Flyway + frontend Angular/NgRx
- [ ] **Phase 3** — Dashboard, refresh tokens, publication d'images GHCR
