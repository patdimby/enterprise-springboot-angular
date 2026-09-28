# enterprise-springboot-angular

Application d'entreprise **gestion de projets** : API REST **Spring Boot 4.1** (Java 21,
Spring Security 7 + JWT, MySQL 8, Flyway, Swagger) et frontend **Angular 20 + NgRx**
(en phase 2). Orchestration **Docker Compose**, CI **GitHub Actions**.

> 📐 Architecture détaillée : [ARCHITECTURE.md](ARCHITECTURE.md)

## Démarrage rapide (dev local)

> 🔐 Les secrets (mot de passe MySQL, secret JWT) ne sont **plus** dans un fichier
> `.env` : ils sont stockés dans **Infisical** et injectés par la CLI au démarrage.
> Configuration à faire une fois : voir la section [Secrets avec Infisical](#-secrets-avec-infisical-plus-de-env-sur-le-disque).

```bash
# 1. Base de données MySQL (profil "dev" du backend)
infisical run --env=dev -- docker compose up -d mysql

# 2. Backend (Maven 3.9+ et JDK 21 requis — profil "dev" actif par défaut)
cd backend
infisical run --env=dev -- mvn spring-boot:run
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

## 🔐 Secrets avec Infisical (plus de `.env` sur le disque)

Le dépôt **ne stocke plus aucun secret** : les mots de passe et le secret JWT vivent
réellement dans un projet **Infisical**, et sont injectés au démarrage par la CLI.
L'application continue de lire ses variables d'environnement habituelles
(`MYSQL_PASSWORD`, `APP_JWT_SECRET`, …) : **aucun code Java ne change**.

### Variables attendues (noms uniquement — jamais de valeur dans Git)

| Variable | Lue par | Rôle |
|---|---|---|
| `MYSQL_USER`, `MYSQL_PASSWORD` | profils `dev` et `docker` | compte applicatif MySQL |
| `MYSQL_ROOT_PASSWORD` | `docker compose` | administration du conteneur MySQL |
| `MYSQL_URL` | profil `prod` uniquement | URL JDBC complète |
| `APP_JWT_SECRET` | tous les profils | clé de signature JWT (≥ 256 bits) |
| `APP_CORS_ALLOWED_ORIGINS` | tous les profils | origines Angular autorisées |
| `SPRING_PROFILES_ACTIVE` | `docker compose` | `docker` pour le backend conteneurisé |

### 1. Créer le projet Infisical (une fois)

1. Créer un compte sur <https://app.infisical.com>.
2. **Secrets Management → + Add New Project** → nom : `enterprise-springboot-angular`.
   Le projet contient déjà les environnements **Development**, **Staging** et **Production**.
3. Sur la page *Secrets Overview* de l'environnement **Development**, **glisser-déposer
   le fichier `.env`** existant : Infisical importe toutes les paires clé/valeur d'un coup.
   (Sinon : `+ Add a New Secret`, une par une.)

Convention utilisée ici : `dev` → Development, `staging` → Staging, `prod` → Production.

### 2. Installer la CLI et s'authentifier

```bash
brew install infisical/get-cli/infisical     # macOS
winget install infisical                     # Windows (aussi : scoop, apt, yum, apk, npm)

infisical login                              # ouvre le navigateur
# WSL 2, Codespaces ou SSH distant (pas de navigateur) :
infisical login -i
```

### 3. Lier le dépôt (une fois par machine)

```bash
infisical init        # choisir le projet, puis l'environnement par défaut
```

`infisical init` écrit **`.infisical.json`** : ce fichier ne contient que les
identifiants du projet (slug, environnement par défaut), **aucun secret**, et il est
**fait pour être commité**.

### 4. Lancer l'application avec les secrets injectés

Toutes les commandes de démarrage sont préfixées par `infisical run --env=dev --` :

```bash
infisical run --env=dev -- docker compose up -d mysql
infisical run --env=dev -- docker compose up --build

cd backend
infisical run --env=dev -- mvn spring-boot:run
```

`infisical run` place les secrets dans l'environnement du processus enfant :
Spring Boot les lit exactement comme avant.

### 5. Vérifier que les secrets ne viennent plus du disque

```bash
# n'affiche QUE la longueur de la variable, jamais sa valeur
# macOS / Linux :
infisical run --env=dev -- sh -c 'echo "APP_JWT_SECRET : ${#APP_JWT_SECRET} caractères"'
# Windows (PowerShell) :
infisical run --env=dev -- pwsh -Command '$env:APP_JWT_SECRET.Length'

# puis : plus aucun secret local
mv .env .env.backup
infisical run --env=dev -- mvn spring-boot:run     # l'app démarre toujours
```

Si l'application démarre alors que `.env` a disparu, les secrets viennent bien d'Infisical.

### 6. CI/CD, Kubernetes et production

Hors poste de développement, **pas de `infisical login` interactif** : créer une
**machine identity** en *Universal Auth*, stocker son client ID / client secret dans le
coffre de la plateforme (ex. *GitHub Actions secrets* pour la CI) et la limiter au strict
projet et environnement nécessaires :

- Machine identities : <https://infisical.com/docs/documentation/platform/identities/machine-identities>
- Universal Auth : <https://infisical.com/docs/documentation/platform/identities/universal-auth>

Les tests de la CI (`mvn verify`) utilisent le profil `test` (base H2 en mémoire) et
n'ont donc besoin d'aucun secret.

### 7. Hygiène

- `.env` est dans `.gitignore` (ainsi que `.env.local` et `.env.backup`).
- **Si un secret a déjà été commité, changez-le** : il reste dans l'historique Git.
  Scanner le dépôt : <https://infisical.com/docs/cli/scanning-overview>.
- Ne jamais coller un secret dans un ticket, un chat ou un fichier suivi par Git.
- Les valeurs de repli encore présentes dans `application-dev.yml` et
  `docker-compose.yml` ne servent qu'au développement sans Infisical : une fois le
  projet Infisical en place, retirez-les (`${MYSQL_PASSWORD}` sans valeur par défaut),
  pour qu'un démarrage sans secrets échoue au lieu d'utiliser un mot de passe écrit
  dans Git.
- `.env.example` ne doit contenir que des **noms** de clés, jamais de valeur.

## Profils de configuration

Un seul fichier `application.yml` contient le commun ; chaque environnement
n'y ajoute que ce qui change (base, secrets, logs) — plus besoin de surcharger
un unique fichier :

| Profil | Fichier | Utilisé quand | Base de données |
|--------|---------|---------------|-----------------|
| `dev` (défaut) | `application-dev.yml` | `mvn spring-boot:run` sans rien préciser | MySQL local `sa` / `Ma$terkey1` |
| `test` | `src/test/resources/application-test.yml` | `mvn test` (automatique) | H2 en mémoire |
| `docker` | `application-docker.yml` | `docker compose up` | MySQL du conteneur `mysql:3306` |
| `prod` | `application-prod.yml` | `SPRING_PROFILES_ACTIVE=prod` | variables d'environnement obligatoires |

Changer de profil :

```bash
# Variable d'environnement (recommandé)
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run

# Ou via Maven
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Le profil `prod` n'a AUCUNE valeur par défaut : `MYSQL_URL`, `MYSQL_USER`,
`MYSQL_PASSWORD`, `APP_JWT_SECRET` et `APP_CORS_ALLOWED_ORIGINS` doivent être
définis, sinon l'application refuse de démarrer (c'est voulu).

## Docker (stack complète)

```bash
# les secrets (MySQL, APP_JWT_SECRET…) sont injectés par Infisical dans
# l'environnement de Docker Compose : aucun .env requis
infisical run --env=dev -- docker compose up --build
```

## Tests

```bash
cd backend
mvn test                # H2 en mémoire, profil "test"
```

## Structure

```
.infisical.json   lien vers le projet Infisical (aucun secret)         ✔ phase 1
backend/          API Spring Boot 4.1 (auth JWT, users, rôles)         ✔ phase 1
frontend/         Angular 20 + NgRx                                    ⏳ phase 2
```

## Roadmap

- [x] **Phase 1** — Squelette backend : sécurité JWT, auth, users, erreurs RFC 7807, Docker, CI
- [ ] **Phase 2** — Entités métier (Project, Task, Comment) + migrations Flyway + frontend Angular/NgRx
- [ ] **Phase 3** — Dashboard, refresh tokens, publication d'images GHCR
