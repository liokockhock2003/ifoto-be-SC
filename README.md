# iFoto Backend

Backend REST API for the **iFoto** application — a photography club / equipment rental platform. Built with Spring Boot 3.5, Java 21, and MySQL, it provides JWT-based authentication, role-based authorization, user management, and modules for events, equipment, and rental pricing.

## Tech Stack

| Concern | Technology |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Mail, Validation) |
| Database | MySQL |
| Migrations | Flyway |
| Auth | JWT (jjwt) — short-lived access token + HttpOnly refresh cookie |
| API docs | springdoc OpenAPI / Swagger UI |
| Build | Maven (wrapper included) |

## Features

- **Authentication** — login, token refresh, logout, registration, and password-reset via email.
- **Authorization** — role-based access (`ROLE_ADMIN`, `ROLE_GUEST`, `ROLE_CLUB_MEMBER`, …) derived from a user's active role.
- **User management** — admin CRUD over users and roles.
- **Events** — event catalogue endpoints.
- **Equipment** — main/sub equipment listings.
- **Rental pricing** — pricing data for equipment rentals.

## Project Structure

```
src/main/java/com/ifoto/ifoto_backend/
├── controller   # @RestController endpoints under /api/v1/
├── service      # Business logic (users, tokens, password reset)
├── repository   # Spring Data JPA interfaces
├── model        # JPA entities (schema managed by Flyway, ddl-auto=none)
├── security     # JwtUtil, JwtAuthenticationFilter, CookieUtil
├── config       # SecurityConfig, AppConfig, WebConfig
└── dto          # Record-based request/response objects
src/main/resources/db/migration   # Flyway V{n}__*.sql migrations
```

## Prerequisites

- JDK 21
- MySQL 8.x running locally
- Git

## Setup

### 1. Create the database and a user

Connect as root (`mysql -u root -p`) and run:

```sql
CREATE DATABASE ifotodb_sc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ifoto_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON ifotodb_sc.* TO 'ifoto_user'@'localhost';
FLUSH PRIVILEGES;
```

> Flyway creates all tables on first startup — you only need the empty database.

### 2. Configure environment variables

The `dev` profile (active by default) reads its config from the environment. Create a `.env` file in the project root:

```env
DEV_DB_URL=jdbc:mysql://localhost:3306/ifotodb_sc?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kuala_Lumpur
DEV_DB_USER=ifoto_user
DEV_DB_PASS=your_password
JWT_SECRET=a-secret-key-at-least-32-characters-long
JWT_EXPIRATION_MS=900000
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=you@example.com
MAIL_PASSWORD=your_app_password
```

| Variable | Purpose |
|---|---|
| `DEV_DB_URL` | JDBC URL for local MySQL |
| `DEV_DB_USER` / `DEV_DB_PASS` | DB credentials |
| `JWT_SECRET` | HS256 signing key (≥ 32 chars) |
| `JWT_EXPIRATION_MS` | Access token TTL in ms (e.g. `900000` = 15 min) |
| `MAIL_*` | SMTP settings for password-reset emails |

> Spring Boot does not auto-load `.env`. Load it into your shell session before running (see below).

### 3. Run the application

**Windows (PowerShell)** — load `.env` then start:

```powershell
Get-Content .env | Where-Object { $_ -match '=' -and $_ -notmatch '^\s*#' } | ForEach-Object { $n,$v = $_ -split '=',2; Set-Item "env:$($n.Trim())" $v.Trim() }
.\mvnw.cmd spring-boot:run
```

**Linux / macOS / WSL:**

```bash
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

## Common Commands

```bash
# Build (skip tests)
./mvnw clean package -DskipTests        # Windows: .\mvnw.cmd ...

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=IfotoBackendApplicationTests

# Flyway (requires DEV_DB_* env vars)
./mvnw flyway:info
./mvnw flyway:repair
```

## API Documentation

Once running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

### Authentication flow

1. `POST /api/v1/auth/login` → access token in body + refresh token in HttpOnly cookie.
2. Send `Authorization: Bearer <token>` on subsequent requests.
3. `POST /api/v1/auth/refresh` → issues a new access token from the cookie.
4. `POST /api/v1/auth/logout` → revokes the refresh token and clears the cookie.

Public routes: `/api/v1/auth/**` and `/api/v1/register`. All other `/api/v1/users/**` routes require `ROLE_ADMIN` (except `PATCH /api/v1/users/{username}/active-role`, which any authenticated user may call).

## Database Migrations

Migrations live in `src/main/resources/db/migration/` using the `V{n}__description.sql` convention. **Never modify an existing migration — always add a new version.**

## Profiles

| Profile | Notes |
|---|---|
| `dev` (default) | Verbose SQL logging; CORS allows `localhost:5173` and `localhost:3000`. Also used by CI (GitHub Actions, Jenkins) to run tests against a disposable MySQL container. |
| `prod` | Configured via `application-prod.properties`; DB and CORS from env vars. |

## Running the pulled Docker image standalone (Task A8)

Teammates who only have the published image (not the source) can run it against a containerized MySQL — no local MySQL install required.

```bash
# 1. Pull the image
docker pull liokockhock2003/ifoto-backend-sc:latest

# 2. Create a shared network so the containers can resolve each other by name
docker network create ifoto-net

# 3. Run MySQL with a named volume (data persists across restarts)
docker run -d --name ifoto-mysql --network ifoto-net \
  -e MYSQL_DATABASE=ifotodb_dev \
  -e MYSQL_USER=ifoto_user \
  -e MYSQL_PASSWORD=your_password \
  -e MYSQL_ROOT_PASSWORD=root_password \
  -v ifoto-mysql-data:/var/lib/mysql \
  mysql:8.0

# 4. Run the backend on the same network, pointed at the MySQL container by name
docker run -d --name ifoto-backend-sc --network ifoto-net -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DEV_DB_URL="jdbc:mysql://ifoto-mysql:3306/ifotodb_dev?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kuala_Lumpur" \
  -e DEV_DB_USER=ifoto_user \
  -e DEV_DB_PASS=your_password \
  -e JWT_SECRET=a-secret-key-at-least-32-characters-long \
  -e JWT_EXPIRATION_MS=900000 \
  liokockhock2003/ifoto-backend-sc:latest
```

Then verify at `http://localhost:8080/swagger-ui.html` (or via Postman).

> The image itself contains no secrets or environment-specific config — everything is supplied at `docker run` time via `-e` flags (or `--env-file .env`, kept local and never committed). MySQL runs in its own container rather than on the host, since a container's `localhost` isn't the host machine's `localhost` — running MySQL in a container on the same Docker network avoids that platform-dependent networking entirely.
