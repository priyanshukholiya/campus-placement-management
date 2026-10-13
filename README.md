# Campus Placement Management System

A local Java microservices project with a React dashboard. This first milestone implements the complete **publish drive → check eligibility → apply → shortlist → interview → selection** workflow, with audited status changes and reliable in-app notifications.

**GitHub is intentionally unlinked.** No repository, remote, commit, or push has been created for this project.

## What works

- Student, placement-officer and recruiter demo accounts, with authorization in each service.
- Student profiles and officer-controlled academic verification.
- Placement-drive publishing and closing, branch filters, and company/role search.
- Server-side eligibility decisions: verification, branch, batch, CGPA, backlogs, open status, and deadline.
- Duplicate-application protection enforced by a database unique constraint.
- Student/recruiter ownership checks and a controlled application state machine.
- Interview time and meeting URL, an immutable academic/eligibility snapshot, and status history.
- Application and notification events committed together using an outbox.
- Retryable notification delivery, idempotent inbox writes, and mark-as-read actions.
- Reports distinguish offers from distinct students selected.
- Separate persistent service databases, Flyway migrations, a gateway, and container configuration.

## Run locally on Windows

Requirements: JDK **21 or newer** and Node.js **22.12 or newer**. The project targets Java 21 and was developed with the installed JDK 25.0.1. A Maven wrapper is included; global Maven is unnecessary. The first build downloads dependencies.

Open PowerShell in this folder:

```powershell
./scripts/build.ps1 -JavaHome 'C:/Program Files/Java/jdk-25'
./scripts/start.ps1 -JavaHome 'C:/Program Files/Java/jdk-25'
```

Use your actual JDK installation path if it differs. The start script also accepts `JAVA_HOME` and can discover a JDK under `Program Files/Java`. It checks the required ports and waits for all service health endpoints and the frontend before printing `Services ready`. Allow about 30–60 seconds for startup.

Open **http://127.0.0.1:5173** after startup completes. Readiness is available at `http://127.0.0.1:8080/actuator/health`; each business service has its own `/actuator/health` endpoint.

| Username | Role / purpose |
|---|---|
| `student` | Aarav Sharma, CSE, 8.40 CGPA; eligible for the seeded drives |
| `student2` | Meera Patel, ECE, 6.20 CGPA; shows both eligible and ineligible outcomes |
| `officer` | Publish drives, verify academics, manage all applications |
| `recruiter` | Review applications for drives assigned to this recruiter |

The default password for all four demo accounts is **`placement-demo`**. Set `DEMO_PASSWORD` in the shell before starting to override it; the login form must then use that value. The internal service account has a separate `SERVICE_PASSWORD`.

Local data persists under `data/`; logs and process ownership records are under `.run/`. Stop the project with:

```powershell
./scripts/stop.ps1
```

For environments that terminate detached child processes, use `./scripts/start.ps1 -Wait` and keep that PowerShell session open. The stop script checks PID and start time so that it does not stop a different process that reused a PID.

## Run with Docker

Start Docker Desktop with Linux containers, then:

```powershell
docker compose up --build -d
```

The same UI is at http://127.0.0.1:5173. Stop the local PowerShell version first because the ports overlap.

- Local mode uses four file-backed H2 databases and authenticated HTTP outbox delivery.
- Docker mode uses PostgreSQL with a database and account per business service, plus RabbitMQ for outbox delivery.
- Docker's gateway and frontend ports bind to loopback. RabbitMQ management is at http://127.0.0.1:15672 (`campus` / `campus-local`). PostgreSQL and business-service ports are not published.
- Persistent Docker volumes retain data when you run `docker compose down`.
- `infra/init-databases.sql` only runs when the PostgreSQL volume is first created.

These are development configurations with explicit demo credentials. They are **not a production deployment**.

## Architecture

```text
React dashboard → Spring Cloud Gateway :8080
                     ├── Student Service      :8081 → student database
                     ├── Recruitment Service  :8082 → recruitment database
                     ├── Application Service  :8083 → application database + outbox
                     └── Notification Service :8084 → notification database

Application Service → authenticated student/drive snapshot APIs
Outbox → HTTP (local) or RabbitMQ (Docker) → idempotent notification inbox
```

| Module | Responsibility |
|---|---|
| `common` | API error format, internal snapshot/event contracts, demo security |
| `student-service` | Profiles and officer-verified academics |
| `recruitment-service` | Drives, requirements, recruiter assignment |
| `application-service` | Eligibility, applications, interviews, audit history, reports, outbox |
| `notification-service` | Private notification inbox and duplicate-event detection |
| `api-gateway` | Routing and request authentication |
| `frontend` | Responsive React application |

Applications never query other services' database tables. Eligibility is checked against the records read at submission time and saved as a snapshot. Profile changes afterward do not alter past decisions. This first version does not implement a distributed lock across drive closure and application submission; a closure racing with submission may be evaluated from the previously read drive snapshot.

## Reliability behavior

Saving an application or a status change inserts its audit and outbox rows in the same transaction. The dispatcher retries delivery with capped exponential backoff. After eight failed attempts, the event is retained as `FAILED`; an officer can inspect `/api/outbox` and retry using `POST /api/outbox/retry`.

Notification IDs equal event IDs, so repeating a delivery does not create another notification. RabbitMQ mode uses durable queues, publisher confirms, mandatory returns, bounded consumer retries and a dead-letter queue (`campus.notifications.failed`). Failed consumer events require review and requeue in RabbitMQ management after the underlying problem is fixed.

The outbox dispatcher currently supports **one Application Service instance**. Add row leasing/locking before running multiple replicas. Reports and list endpoints currently return the small demo dataset without server pagination.

## Verify

```powershell
./mvnw.cmd verify
cd frontend
npm ci
npm run build
npm audit
```

The build script runs Java tests and the frontend build with caches and download temporary files kept inside `.tools/`. Run `npm audit` separately to check frontend dependencies. Test JVMs receive Mockito as a Java agent explicitly, avoiding dynamic self-attachment on newer JDKs.

Tests cover eligibility boundaries, deadline exclusion, multiple rejection reasons, terminal states, transaction side effects, concurrent duplicate submissions, dependency failure, ownership and roles, full status progression, and duplicate notification delivery.

Run `node scripts/smoke.mjs` against a running local stack for the cross-service smoke test. Alternatively, `./scripts/start.ps1 -Smoke -Wait` starts the stack, waits for readiness, runs the checks, and keeps the services running. The checks create synthetic records in the current local database. See [docs/VALIDATION.md](docs/VALIDATION.md) for verified results and environment limitations.

See [docs/API.md](docs/API.md) for the API map and [docs/ROADMAP.md](docs/ROADMAP.md) for the next milestones.

## Security and next steps

This milestone uses explicitly labeled **local demo authentication**: HTTP Basic, in-memory accounts, stateless requests, and loopback bindings. The frontend keeps credentials only in memory and clears them on logout. These accounts are not registration or production identity management.

Before using real student data or exposing the system beyond your computer, complete Keycloak/OIDC login, service credentials, HTTPS, secret management, and access review. Résumé upload/download and real email delivery are not implemented yet. All included records are fictional.
