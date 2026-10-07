# Little Home School

A school operations platform starter for seven branches and five early-years classes, with a responsive Next.js dashboard, Spring Boot API foundation, PostgreSQL schema, and local observability stack.

## Run the web app

```bash
npm install
npm run dev
```

Open <http://localhost:3000>. The overview is a front-end prototype with representative data. Branch selection, theme, chart range, student creation, search, and CSV export work locally in the browser; they are not persisted or connected to a signed-in account yet.

To run the API in Compose while using the Next.js dev server, set `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1` in `.env.local` before starting Next.js. The API is published only on loopback for local development.

## Run the full local stack

Copy `.env.example` to `.env`, replace the values, and set `JWT_SECRET_BASE64` using `openssl rand -base64 48`; then run:

```bash
docker compose up --build
```

For the first admin only, add `BOOTSTRAP_ADMIN_ENABLED=true`, `BOOTSTRAP_ADMIN_EMAIL`, and a unique `BOOTSTRAP_ADMIN_PASSWORD` of at least 16 characters to the ignored `backend/.env`, then start Spring Boot once. Remove/disable those bootstrap settings immediately afterward. The bootstrap does nothing if any user already exists. Also set `JWT_SECRET_BASE64` in `backend/.env`; the API intentionally has no built-in signing-key fallback.

- App via Nginx: <http://localhost:8088>
- Swagger UI: <http://localhost:8088/swagger-ui.html>
- Prometheus: <http://localhost:9090>
- Grafana: <http://localhost:3001>

Compose credentials/defaults are for local development only. Never deploy these defaults. The initial migration creates the seven branches, roles, classes, and domain tables; it deliberately does not create a login user or sample child records. A secure bootstrap-admin workflow is required before first login.

## Build and checks

```bash
npm run lint
npm run build
mvn -f backend/pom.xml verify
```

The backend targets Java 21 and Spring Boot 3.5.6. Local Java 21 is recommended; Maven compiles with `--release 21`. Schema changes belong in `backend/src/main/resources/db/migration` and Hibernate is configured to validate, not create, the schema. Use [backend/.env.example](backend/.env.example) as the key template; the real `backend/.env` is ignored.

For a local backend launch from the repository root, Spring loads `backend/.env` when present. Keep real credentials in that ignored file, and do not paste them into chat or commit them. The Compose deployment supplies datasource variables directly and does not copy dotenv files into its images.

Redis health is disabled by default for a standalone local API because Redis-backed features are not active yet; Compose enables the Redis health check because it starts Redis alongside the API.

## Architecture and contracts

- [System architecture, HLD/LLD, roles, ER diagram, and auth flow](docs/architecture.md)
- [REST API routes, DTO examples, validation, and error contract](docs/api.md)
- [Delivery roadmap and production readiness criteria](docs/roadmap.md)
- PostgreSQL DDL and seed reference data: `backend/src/main/resources/db/migration/V1__school_management_schema.sql`

The executable API slice includes login, refresh-token rotation, logout, scoped branch listing, and branch-scoped student search/admission/profile read/update. Swagger is enabled and reflects implemented controllers. Other domain routes in the API document are contracts/plans and are not implemented controllers. Guardian linking, attendance, learning, communication, reporting, document, and audit services still need feature use cases and authorization tests before this is production-ready.

## Security and deployment notes

- Access JWTs are short-lived; refresh tokens are random, stored only as SHA-256 hashes, and rotated/revoked in PostgreSQL.
- The sample API returns tokens in JSON for local integration. Production browser sessions should move refresh tokens to secure HttpOnly SameSite cookies and add CSRF protection, rate limits, password reset, key rotation, and login abuse monitoring.
- Branch authorization must be checked in application services and repository predicates for every resource. Never trust a client-supplied branch ID as authorization.
- Use managed PostgreSQL/Redis, encrypted private object storage, TLS termination, managed secrets, point-in-time backups, tested restores, log redaction, and deployment-specific CORS in cloud environments.
- Nginx, Prometheus, and Grafana Compose configuration is a local deployment reference. Production AWS/Azure deployment should use managed database/cache/object storage, an orchestrator, a WAF/load balancer, private subnets, and organization-approved observability.
- [AWS/Azure production deployment and release strategy](docs/deployment.md)

## Local test accounts

The local-only seeder created these accounts in the configured development database. They are for local testing only; do not use them in staging or production.

| Role | Email | Password |
|---|---|---|
| Super Admin | `superadmin@test.littlehome.local` | `Local-Only-LHS-SuperAdmin-2026!` |
| Branch Admin | `branchadmin@test.littlehome.local` | `Local-Only-LHS-BranchAdmin-2026!` |
| Teacher | `teacher@test.littlehome.local` | `Local-Only-LHS-Teacher-2026!` |
| Parent | `parent@test.littlehome.local` | `Local-Only-LHS-Parent-2026!` |

The Parent account is linked to `TEST-CHILD-001` (“Test Learner”). To recreate missing accounts, start with `SPRING_PROFILES_ACTIVE=local` and `TEST_ACCOUNTS_ENABLED=true` plus the four `TEST_*_PASSWORD` variables from `backend/.env.example`. The seeder is disabled by default and only runs in the `local` Spring profile. Change the test passwords if this database is reachable by anyone besides you.
