# Architecture — `enterprise-springboot-angular`

Monorepo d'une application d'entreprise type **gestion de projets**, avec un backend
**Spring Boot 4.1** (Java 21, Spring Security 7, JWT) et un frontend **Angular 22**
(Material 3, standalone, zoneless, signals), une base **MySQL 8**, le tout orchestré
par **Docker Compose**.

> Ce document est le plan de construction du dépôt. Chaque section décrit ce qui
> existe (✔ créé) ou ce qui restera à créer (⏳ phases suivantes).

---

## 1. Vue d'ensemble

```
                        ┌──────────────────────────────────────────────┐
                        │                Docker Compose                │
                        │                                              │
  Navigateur ──HTTP──▶  │  ┌────────────┐   ┌──────────────┐   ┌─────┐ │
   (Angular SPA)        │  │  frontend  │──▶│   backend    │──▶│MySQL│ │
                        │  │ nginx:80   │   │ Spring Boot  │   │ 8.4 │ │
                        │  └────────────┘   │   :8080      │   └─────┘ │
                        │        ▲          └──────────────┘           │
                        │        │ (dev: ng serve :4200 → proxy /api)  │
                        └────────┼─────────────────────────────────────┘
                                 ▲
                 Admins ──▶ http://localhost:8080/swagger-ui.html
```

| Couche | Technologie | Version |
|---|---|---|
| Backend | Spring Boot (webmvc, data-jpa, security, validation, actuator) | **4.1.0** |
| Langage | Java | 21 (LTS) |
| Sécurité | Spring Security 7 + JJWT (jjwt-api/impl/jackson) | 7.0.x / **0.12.6** |
| Persistance | Spring Data JPA (Hibernate), Flyway, MySQL Connector/J | Boot-managé |
| API docs | springdoc-openapi (Swagger UI) — contrat testé par `OpenApiDocsTests` | **3.1.1** |
| Mapping | MapStruct + Lombok | **1.6.3** / 1.18.42 |
| Frontend | Angular (standalone, zoneless, signals) + Angular Material 3 | **22.2** |
| State | Signals Angular (AuthStore) + computed ; NgRx envisagé pour le domaine métier | — |
| Tests frontend | Vitest (unitaires/fonctionnels, jsdom) + Playwright (e2e) | 5.x / 1.5x |
| Base de données | MySQL | 8.4 LTS |
| Conteneurisation | Docker Compose, build multi-stage, Jib-like layering | — |
| CI | GitHub Actions (build + tests + images) | — |

---

## 2. Arborescence du dépôt

```
enterprise-springboot-angular/
├── ARCHITECTURE.md              ← ce document
├── README.md
├── .gitignore
├── .env.example                 ← liste des clés de secrets (aucune valeur réelle)
├── .infisical.json              ← (après `infisical init`) lien projet, sans secret
├── docker-compose.yml           ← mysql + backend + frontend
├── .github/workflows/ci.yml
│
├── backend/                                ✔ créé (phase 1)
│   ├── pom.xml                             ← toutes les dépendances, dernières versions
│   ├── Dockerfile                          ← multi-stage (build Maven → JRE temurin)
│   └── src/
│       ├── main/java/com/enterprise/app/
│       │   ├── EnterpriseApplication.java
│       │   ├── config/                     ← SecurityConfig, OpenApiConfig, AppProperties,
│       │   │                                 CorsConfig, DataInitializer
│       │   ├── security/                   ← JwtService, JwtAuthenticationFilter,
│       │   │                                 JwtAuthEntryPoint, JwtAccessDeniedHandler
│       │   ├── auth/                       ← AuthController, AuthService, DTOs
│       │   ├── user/                       ← User, Role, repositories, service, controller
│       │   ├── project/                    ← (⏳) entités métier
│       │   ├── task/                       ← (⏳)
│       │   ├── common/                     ← (⏳) erreurs, pagination, exceptions
│       │   └── ...
│       ├── main/resources/
│       │   ├── application.yml             ← commun à tous les profils (défaut : dev)
│       │   ├── application-dev.yml         ← profil développement (MySQL local sa/Ma$terkey1)
│       │   ├── application-docker.yml      ← profil conteneur (host mysql)
│       │   ├── application-prod.yml        ← modèle production (variables d'env)
│       │   └── db/migration/               ← (⏳) migrations Flyway V1__*.sql
│       └── test/
│           ├── java/...                    ← tests @SpringBootTest (H2)
│           └── resources/application-test.yml
│
├── frontend/                               ✔ créé (phase 2a — Angular 22)
│   ├── package.json, angular.json, tsconfig.*.json
│   ├── proxy.conf.json                     ← dev : /api → http://localhost:8080
│   ├── playwright.config.ts                ← e2e (démarre ng serve automatiquement)
│   ├── Dockerfile                          ← build Angular → nginx alpine
│   ├── nginx.conf                          ← SPA fallback + proxy /api
│   ├── e2e/                                ← tests fonctionnels e2e Playwright
│   └── src/
│       ├── environments/                   ← apiUrl /api (dev : via proxy)
│       └── app/
│           ├── app.ts|html|scss            ← coquille : toolbar + sidenav + router
│           ├── app.config.ts               ← zoneless, http+interceptor, router
│           ├── app.routes.ts               ← routes lazy-loadées + guards
│           ├── core/
│           │   ├── models.ts               ← types TS = DTOs Java (LoginResponse...)
│           │   ├── auth-store.ts           ← état session (signals + localStorage)
│           │   ├── auth-interceptor.ts     ← en-tête Bearer + logout auto sur 401
│           │   ├── auth-guards.ts          ← authGuard, adminGuard
│           │   ├── auth-api.ts, users-api.ts ← clients HTTP
│           │   └── *.spec.ts               ← tests unitaires (Vitest)
│           └── features/
│               ├── home/                  ← accueil + sonde santé API
│               ├── auth/                  ← login, register (Reactive Forms)
│               ├── users/                 ← admin : table, rôles, activation
│               └── forbidden/             ← page 403 maison
│
└── docs/                                   ⏳ (phase 3) screenshots, ADR, guide de contribution
```

---

## 3. Backend Spring Boot — architecture en couches

Organisation **par domaine** (package-per-feature), chaque module suivant la même
structure verticale : `controller → service → repository → entity`, avec DTOs et
mapper dédiés. Les dépendances pointent toujours vers le bas ; les modules se
parlent via leurs interfaces de service.

```
controller (REST, @RestController, validation des DTOs)
    │  records DTO : *Request / *Response (jamais d'entités exposées)
    ▼
service (logique métier, transactions, interface + impl)
    │
    ▼
repository (Spring Data JPA, dérivation de requêtes)
    │
    ▼
entity (JPA : User, Role, Project, Task, ...)
```

### 3.1 Entités (créées ou prévues)

| Entité | Rôle | Relations |
|---|---|---|
| `User` | Compte applicatif | N-N `Role` |
| `Role` | Rôles métier : `ADMIN`, `MANAGER`, `USER` | N-N `User` |
| `Project` ⏳ | Projet | N-1 `User` (owner), N-N `User` (members) |
| `Task` ⏳ | Tâche assignée | N-1 `Project`, N-1 `User` (assignee), énum `TaskStatus` |
| `Comment` ⏳ | Commentaire | N-1 `Task`, N-1 `User` |

### 3.2 Schéma MySQL

```sql
users(id PK, email UNIQUE, password_hash, full_name, enabled, created_at, updated_at)
roles(id PK, name UNIQUE)             -- ADMIN, MANAGER, USER
users_roles(user_id FK, role_id FK)   -- table de jointure
projects(id PK, name, description, owner_id FK→users, created_at)     ⏳
project_members(project_id FK, user_id FK)                            ⏳
tasks(id PK, title, description, status ENUM, project_id FK,          ⏳
      assignee_id FK, due_date, created_at, updated_at)               ⏳
comments(id PK, content, task_id FK, author_id FK, created_at)        ⏳
```

### 3.3 API REST (contrat)

Préfixe global `/api`. Réponses paginées : `PageResponse<T> { content, page, size, totalElements, totalPages }`.
Erreurs : JSON RFC 7807 via `ProblemDetail` (`/api/...` → handler `@RestControllerAdvice`).

| Méthode | Route | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Inscription (rôle USER) |
| POST | `/api/auth/login` | public | Connexion → `LoginResponse{token, type, id, email, fullName, roles}` |
| GET | `/api/auth/me` | JWT | Profil courant |
| GET | `/api/users` | ADMIN | Liste paginée |
| GET | `/api/users/{id}` | ADMIN | Détail |
| PATCH | `/api/users/{id}/roles` | ADMIN | Modifier les rôles |
| DELETE | `/api/users/{id}` | ADMIN | Supprimer un compte |
| GET/POST/PUT/DELETE | `/api/projects/**` ⏳ | JWT | CRUD projets (+ `/my`, `/members`) |
| GET/POST/PUT/DELETE | `/api/tasks/**` ⏳ | JWT | CRUD tâches (+ filtres par statut/assigné) |
| POST/DELETE | `/api/tasks/{id}/comments/**` ⏳ | JWT | Commentaires |

**Exemple — POST /api/auth/login**

```json
// requête
{ "email": "admin@enterprise.com", "password": "Admin123!" }
// réponse 200
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "admin@enterprise.com",
  "fullName": "Admin",
  "roles": ["ADMIN"]
}
```

### 3.4 Sécurité JWT (Spring Security 7)

Flux : `AuthController` → `AuthService` → `AuthenticationManager` (DAO, `BCryptPasswordEncoder`)
→ `JwtService` signe un HS256 (clé Base64 ≥ 256 bits, expiration configurable).

Sur chaque requête : `JwtAuthenticationFilter` (une fois par requête, dans
`SecurityConfig`) lit l'en-tête `Authorization: Bearer …`, valide signature + expiration,
et pose l'`Authentication` dans le `SecurityContext`.

`SecurityConfig` (DSL Lambda, obligatoire en Security 7) :
- CSRF désactivé (API stateless), `SessionCreationPolicy.STATELESS`, CORS configuré ;
- publics : `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health/**` ;
- `/api/users/**` → `hasRole("ADMIN")` ; le reste de `/api/**` authentifié ;
- gestion d'erreurs JSON : `JwtAuthEntryPoint` (401) et `JwtAccessDeniedHandler` (403) ;
- `@EnableMethodSecurity` pour les annotations `@PreAuthorize` au cas par cas.

Secret et durées dans `AppProperties` (`@ConfigurationProperties("app")`), injectés
depuis les variables d'environnement (`APP_JWT_SECRET`, `APP_JWT_EXPIRATION_MS`, …).

### 3.5 Conventions de code

- DTOs = `record` Java immuables ; validation `jakarta.validation` sur les requêtes ;
- MapStruct pour entité ↔ DTO (compilation Maven, `componentsModel = spring`);
- Lombok `@Getter/@Setter/@Builder/...` sur les entités uniquement ;
- colonnes `created_at`/`updated_at` via `@CreationTimestamp`/`@UpdateTimestamp` ;
- tests : `@SpringBootTest` + H2 en mémoire (profil `test`), AssertJ ;
- nommage : `XxxController`, `XxxService`(+`Impl`), `XxxRepository`, `XxxMapper`, DTO `XxxRequest/Response`.

---

## 4. Frontend Angular 22 ✔ (phase 2a)

**Stack** : composants standalone, **zoneless** (`provideZonelessChangeDetection`),
state par **signals**, `provideHttpClient(withInterceptors([authInterceptor]))`,
`provideRouter` avec transitions de vues, Angular **Material 3** (`mat.theme()`).

```
src/app/
├── core/                       ← l'infra transverse (en dehors des écrans)
│   ├── models.ts               : types TS miroirs des DTO Java
│   ├── auth-store.ts           : session = signal persisté (localStorage)
│   │                             + valeurs dérivées (isLoggedIn, isAdmin)
│   ├── auth-interceptor.ts     : ajout "Authorization: Bearer" ; sur 401 →
│   │                             logout + redirect /login (functional interceptor)
│   ├── auth-guards.ts          : authGuard (returnUrl), adminGuard → /forbidden
│   └── auth-api.ts, users-api.ts : les seuls endroits qui connaissent les URLs
└── features/                   ← un dossier par page (lazy-loadée)
    ├── home/                   : cartes session + santé API (GET /actuator/health,
    │                             rafraîchi toutes les 30 s, point vert/rouge)
    ├── auth/                   : login (returnUrl), register (confirmation mdp,
    │                             enchaînement auto register → login)
    ├── users/                  : ADMIN — MatTable paginée côté serveur,
    │                             recherche locale, dialog rôles, toggle enabled,
    │                             suppression confirmée, snack-bars de feedback
    └── forbidden/              : page 403 (redirigée par adminGuard)
```

**Design** : thème Material 3 via `mat.theme()` (palettes azure/blue) sur `html`,
variables `--mat-sys-*` ; mode sombre = classe `.dark-mode` sur `<body>`
(`color-scheme: dark`) avec bascule persistée ; layout responsive (sidenav
`side` ≥ 960 px, `over` en dessous, grille `auto-fit` pour les cartes).

**Tests** :
- Vitest (`*.spec.ts`, jsdom, builder `@angular/build:unit-test`) : stores,
  guards, intercepteur, clients HTTP (HttpTestingController), composants
  (formulaire de login, tableau users, santé) — le DOM réel est vérifié ;
- Playwright (`e2e/`) : parcours navigateur réel — login → accueil, /users
  admin, 403 pour un USER, responsive mobile ; `/api/**` est intercepté
  (`page.route`) pour tourner sans backend.

### 4.1 Évolutions prévues (phase 2b, écrans projets/tâches) ⏳

Le domaine métier pourra adopter NgRx (store/entity/effects) si la complexité
le justifie ; l'infrastructure actuelle (signals + intercepteur + guards) reste
valable telle quelle.

---

## 5. MySQL & migrations

- Conteneur `mysql:8.4` (volume `mysql_data`), base `enterprise_db`, healthcheck `mysqladmin ping`.
- Backend : `spring.jpa.hibernate.ddl-auto=validate` + **Flyway** (`db/migration/V1__init.sql`
  crée les tables de la section 3.2, `V2__seed.sql` insère l'admin).
  En phase 1, la génération de schéma Hibernate est utilisée ; les migrations
  Flyway remplaceront `ddl-auto` dans la phase métier (projet/tâches).
- Profils : `dev` (local, MySQL dockerisé), `docker` (conteneur, host `mysql`), `prod` (modèle).

---

## 6. Docker

- **docker-compose.yml** : 3 services
  - `mysql` : 8.4, healthcheck, volume nommé ;
  - `backend` : image construite par `backend/Dockerfile` (multi-stage : `maven:3.9-eclipse-temurin-21`
    → `eclipse-temurin:21-jre-alpine`), dépend de `mysql:condition: service_healthy`,
    variables d'env pour le secret JWT et la BDD, healthcheck `/actuator/health` ;
  - `frontend` ✔ : image construite par `frontend/Dockerfile` (multi-stage
    `node:22-alpine` → `nginx:1.27-alpine`), `nginx.conf` avec fallback SPA et
    `proxy_pass /api → backend:8080` (une seule origine : pas de CORS en prod).
- Réseau interne `app-net` ; seules les ports 80 (frontend) et 8080 (backend, dev) sont exposés.
- **Secrets : Infisical** — plus aucun `.env` sur le disque. Les variables
  (`MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `MYSQL_URL`,
  `APP_JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`) vivent dans un projet Infisical
  (environnements Development / Staging / Production) et sont injectées par la CLI :
  `infisical run --env=dev -- docker compose up --build`. Détails et procédure
  d'installation : README, section « 🔐 Secrets avec Infisical ».

---

## 7. CI GitHub Actions

`.github/workflows/ci.yml` : sur `push`/`pull_request` →
1. job **backend** : `actions/setup-java` (Temurin 21, cache Maven) + `mvn verify` ;
2. job **frontend** : Node 22 + `npm ci` + `npm run build` + `npm test` (Vitest) ;
3. job **e2e** : Playwright (`npx playwright install --with-deps chromium`),
   le config démarre `ng serve` (webServer) ; appels `/api` interceptés ;
4. job **docker** : build des images backend + frontend (sans push) ;
4. **Secrets** : les tests CI (`mvn verify`, profil H2) n'ont besoin d'aucun secret.
   Si un job en exige plus tard, pas de `infisical login` interactif : une *machine
   identity* en **Universal Auth** (limitée au projet et à l'environnement requis),
   dont le *client ID* / *client secret* vit dans les *GitHub Actions secrets*.
   Voir README, section « 🔐 Secrets avec Infisical ».

---

## 8. Ordre de construction

1. ✔ **Phase 1 (fait)** : squelette backend — `pom.xml` (dernières versions), config,
   sécurité JWT, auth, users, gestion d'erreurs, tests, Docker, CI.
2. ✔ **Phase 2a (fait)** : frontend Angular 22 complet — auth (login/register),
   admin users, Material 3 responsive, santé API, tests Vitest + Playwright, Docker.
3. ⏳ Phase 2b : entités métier (`Project`, `Task`, `Comment`), migrations Flyway,
   endpoints CRUD complets, écrans projets/tâches.
3. ⏳ Phase 3 : dashboard, rafinements (refresh tokens, pagination généralisée, Docker push GHCR).
