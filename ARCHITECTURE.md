# Architecture — `enterprise-springboot-angular`

Monorepo d'une application d'entreprise type **gestion de projets**, avec un backend
**Spring Boot 4.1** (Java 21, Spring Security 7, JWT) et un frontend **Angular 20**
(NgRx), une base **MySQL 8**, le tout orchestré par **Docker Compose**.

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
| API docs | springdoc-openapi (Swagger UI) | **3.1.1** |
| Mapping | MapStruct + Lombok | **1.6.3** / 1.18.42 |
| Frontend | Angular (standalone) + Angular Material | 20.x |
| State | NgRx (store, effects, entity, devtools) | 20.x |
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
├── .env.example                 ← variables docker-compose (copier en .env)
├── docker-compose.yml           ← mysql + backend + frontend
├── .github/workflows/ci.yml
│
├── backend/                                ✔ créé (phase 1)
│   ├── mvnw, mvnw.cmd, .mvn/wrapper/       ← Maven Wrapper 3.9.16 (aucune install requise)
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
│       │   ├── application.yml             ← profil par défaut
│       │   ├── application-docker.yml      ← profil conteneur
│       │   ├── application-prod.yml        ← modèle production
│       │   └── db/migration/               ← (⏳) migrations Flyway V1__*.sql
│       └── test/
│           ├── java/...                    ← tests @SpringBootTest (H2)
│           └── resources/application-test.yml
│
├── frontend/                               ⏳ (phase 2 — Angular 20 + NgRx)
│   ├── package.json, angular.json
│   ├── Dockerfile                          ← build Angular → nginx alpine
│   ├── nginx.conf                          ← SPA fallback + proxy /api
│   └── src/app/
│       ├── core/                           ← interceptors JWT, guards, services API
│       ├── shared/                         ← composants UI réutilisables
│       ├── features/
│       │   ├── auth/                       ← login/register + NgRx auth state
│       │   ├── projects/                   ← NgRx projects state (entity + effects)
│       │   ├── tasks/                      ← NgRx tasks state
│       │   └── dashboard/
│       └── app.config.ts, app.routes.ts
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

## 4. Frontend Angular + NgRx ⏳

Angular 20 (standalone, `provideHttpClient(withInterceptors(...))`, `provideRouter`).

```
src/app/
├── core/
│   ├── auth/  (AuthService, authInterceptor (401→logout), authGuard, roleGuard)
│   └── api/   (ApiClient de base, intercepteur Bearer, gestion erreurs)
├── shared/    (composants UI, pipes, directives)
└── features/
    ├── auth/       : login/register + store NgRx { user, token, error }
    ├── projects/   : store NgRx — actions/loadProjects, reducer, selectors,
    │                 effects (ProjectsEffects → ProjectsService → HttpClient)
    │                 liste + détail + formulaire (Material)
    ├── tasks/      : EntityAdapter (tasks par projet), filtres statut
    └── dashboard/  : statistiques (charts)
```

State NgRx par feature : `state.ts` (interface + `createEntityAdapter`), `actions.ts`,
`reducer.ts`, `selectors.ts`, `effects.ts`. `provideStore`, `provideEffects`,
`provideStoreDevtools` dans `app.config.ts`. Le token JWT est conservé en
`localStorage`, l'intercepteur l'ajoute à chaque requête et déconnecte sur 401.

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
  - `frontend` ⏳ : image construite par `frontend/Dockerfile` (build `ng build` → nginx),
    `nginx.conf` avec fallback SPA et `proxy_pass /api → backend:8080`.
- Réseau interne `app-net` ; seules les ports 80 (frontend) et 8080 (backend, dev) sont exposés.
- `.env.example` : `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`, `APP_JWT_SECRET`, …

---

## 7. CI GitHub Actions

`.github/workflows/ci.yml` : sur `push`/`pull_request` →
1. job **backend** : `actions/setup-java` (Temurin 21, cache Maven) + `mvnw verify` ;
2. job **frontend** ⏳ : Node 20 + `npm ci` + `ng build --configuration production` + tests headless ;
3. job **docker** : build des images (sans push) pour valider les Dockerfiles.

---

## 8. Ordre de construction

1. ✔ **Phase 1 (fait)** : squelette backend — `pom.xml` (dernières versions), config,
   sécurité JWT, auth, users, gestion d'erreurs, tests, wrapper Maven, Docker, CI.
2. ⏳ Phase 2 : entités métier (`Project`, `Task`, `Comment`), migrations Flyway,
   endpoints CRUD complets, frontend Angular 20 + NgRx.
3. ⏳ Phase 3 : dashboard, rafinements (refresh tokens, pagination généralisée, Docker push GHCR).
