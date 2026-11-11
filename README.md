# EventFlow

EventFlow is a multi-tenant event operations platform for professional event planners. The initial architecture is a modular Spring Boot monolith with a React and TypeScript frontend and PostgreSQL.

## Project layout

```text
backend/     Spring Boot API, Maven Wrapper, Flyway, and Dockerfile
frontend/    React, TypeScript, Vite, and Tailwind CSS
compose.yaml Local PostgreSQL and API services
```

## Prerequisites

- Docker Desktop with its Linux container engine running
- Node.js 20.19+ or 22.12+ and npm

The backend container uses Java 21. The Maven Wrapper downloads and runs the Maven version configured by the project, so a separate host Maven installation is not required.

## Start the database and API

1. Copy `.env.example` to `.env` and replace `DB_PASSWORD` with a local password. `.env` is ignored by Git.
2. From the project root, run:

   ```powershell
   docker compose up --build -d
   ```

   Compose waits for PostgreSQL's health check before starting the API.
3. Check the API at [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health). It should return `{"status":"UP"}`.

To stop the services, run `docker compose down`. The PostgreSQL data remains in a named volume. To deliberately remove that local data too, run `docker compose down --volumes`.

## Start the frontend

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite. The Vite development server proxies `/api` requests to the backend on port 8080.

## Verify the project

Building the API image runs the backend JUnit tests before packaging the application:

```powershell
docker compose build api
```

From `frontend/`, run `npm run build` to type-check and build the frontend.

## Configuration and data

- Database credentials are read from environment variables and are not committed.
- Registration creates an unverified account and sends a verification link only when `MAIL_ENABLED=true` and SMTP settings are configured in `.env`. For Gmail, use an App Password rather than the Google account password. Set `APP_BASE_URL` to the frontend origin that will handle `/verify-email` links.
- Verification tokens expire after 24 hours, are single-use, and are stored in the database only as SHA-256 hashes. The email token is sent after the registration transaction commits.
- Flyway owns database schema changes; Hibernate does not create or update tables.
- The initial foundation intentionally has no business tables. Domain tables will arrive with their feature migrations.
- Vite is pinned to version 7 for reliable builds in this Windows workspace; validate Vite 8 path handling before upgrading.
- EventFlow uses one currency per workspace, defaulting to NGN, and `Africa/Lagos` for event times as agreed for the MVP.
- Never use the sample `.env.example` password outside a local development environment.
