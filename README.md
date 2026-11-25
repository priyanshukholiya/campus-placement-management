# Campus Placement Management System

[![CI](https://github.com/priyanshukholiya/campus-placement-management/actions/workflows/ci.yml/badge.svg)](https://github.com/priyanshukholiya/campus-placement-management/actions/workflows/ci.yml)

A Java microservices application with a React dashboard for running campus placement drives. It covers the full **publish drive → check eligibility → apply → shortlist → interview → selection** workflow, with audited status changes and reliable in-app notifications.

Built with Spring Boot 4, Spring Cloud Gateway, PostgreSQL, RabbitMQ and React. All included records are fictional.

## Quick start (Docker — Windows, macOS, Linux)

You need [Docker Desktop](https://www.docker.com/products/docker-desktop/) (or Docker Engine 25+ with the Compose plugin), Git, and about **4 GB of free memory** for Docker.

```bash
git clone https://github.com/priyanshukholiya/campus-placement-management.git
cd campus-placement-management
docker compose up --build -d --wait
```

The first build downloads dependencies and compiles every service, which takes a few minutes. Later starts take under a minute. `--wait` returns when every container reports healthy, so the app is ready as soon as the command finishes.

Open **http://localhost:5173** and sign in with one of the demo accounts below.

### Skip the build: use prebuilt images

Every commit to `main` that passes CI publishes ready-made images for `linux/amd64` and `linux/arm64` (Apple Silicon) to GitHub Container Registry. To use them instead of compiling locally, which takes about a minute to download:

```bash
docker compose -f compose.yaml -f compose.prebuilt.yaml up -d --wait
```

Use the same `-f compose.yaml -f compose.prebuilt.yaml` flags with later `docker compose` commands. Set `IMAGE_TAG=sha-<commit>` to run a specific commit instead of `latest`.

### Stop and reset

To stop the app, run `docker compose down`. Your data is kept in Docker volumes and comes back on the next `docker compose up -d --wait`. To start again from fresh demo data, run `docker compose down -v`.

## Demo accounts

Every account uses the password **`placement-demo`**.

| Username | Who it is | What they can do |
|---|---|---|
| `student` | Aarav Sharma, CSE, CGPA 8.40 | Eligible for both seeded drives; apply, track, withdraw |
| `student2` | Meera Patel, ECE, CGPA 6.20 | Eligible for one drive and ineligible for the other, with the reasons shown |
| `officer` | Placement officer | Publish and close drives, verify academic records, manage every application, view reports |
| `recruiter` | Recruiter assigned to the seeded drives | Review applicants, shortlist, schedule interviews, select or reject |

To use a different password, set `DEMO_PASSWORD` before starting, for example `DEMO_PASSWORD=my-secret docker compose up -d --wait`, or copy `.env.example` to `.env` and edit it.

## Try the workflow

1. Sign in as **`student2`**. *Aster Labs* is shown as ineligible (branch and CGPA), and *Northstar Systems* is eligible. Apply to Northstar.
2. Sign in as **`student`** and apply to *Aster Labs*. Applying a second time is rejected.
3. Sign in as **`recruiter`**. Shortlist the application, then schedule an interview with a future date and an `https://` meeting link.
4. Mark the candidate **Selected** (or Rejected). Each step is added to the application's timeline.
5. Sign back in as **`student`**. The bell icon shows a notification for every status change.
6. Sign in as **`officer`** to see all applications, verify student academics, publish a new drive, and view the placement report.

The seeded drives have deadlines 14 and 21 days after the first start. If they have expired in a long-lived installation, publish a new drive as `officer` or reset the demo data with `docker compose down -v`.

## Useful URLs (Docker)

| URL | What |
|---|---|
| http://localhost:5173 | Web app |
| http://localhost:8080/actuator/health | API gateway health |
| http://localhost:15672 | RabbitMQ management (`campus` / `campus-local`) |

All ports are bound to `127.0.0.1` and are only reachable from your own machine. PostgreSQL and the individual services are not published. See [docs/API.md](docs/API.md) for the REST API.

## Run without Docker (Windows)

This mode runs each service as a local Java process with file-based H2 databases instead of PostgreSQL and RabbitMQ. The launcher scripts are PowerShell scripts, so this mode is **Windows only**. On macOS and Linux, use Docker.

Requirements: JDK **21 or newer** and Node.js **22.12 or newer**. Maven is not needed because the repository includes the Maven wrapper.

```powershell
./scripts/build.ps1     # compiles, runs the tests, installs and builds the frontend
./scripts/start.ps1     # starts all services and waits until they are healthy
./scripts/stop.ps1      # stops only the processes this project started
```

The scripts use `JAVA_HOME`, or find a JDK under `C:\Program Files\Java`. To choose one explicitly, pass `-JavaHome 'C:/path/to/jdk'`. Startup takes 30–60 seconds. Open **http://127.0.0.1:5173** once it prints `Services ready`.

- Data is stored under `data/`, and logs and process records under `.run/`.
- `./scripts/start.ps1 -Smoke` also runs the end-to-end checks after startup.
- If your terminal stops child processes when it closes, use `./scripts/start.ps1 -Wait` and keep that window open.
- Docker and this mode use the same ports. Stop one before starting the other.

## Testing

Unit and integration tests run on every OS:

```bash
./mvnw verify            # macOS / Linux
./mvnw.cmd verify        # Windows
```

End-to-end smoke test against a running stack (Docker or native; needs Node.js 22+):

```bash
node scripts/smoke.mjs
```

It runs 12 checks across services: authentication, drive publishing, eligibility, duplicate protection, ownership and role checks, the full status workflow with audit history, reporting, notification delivery, and drive closure. It creates synthetic records in the current database. Set `CAMPUS_URL` to target a gateway other than `http://127.0.0.1:8080`.

Frontend checks:

```bash
cd frontend
npm ci
npm run build
npm audit
```

GitHub Actions runs all of the above on every push and pull request: backend tests, the frontend build with a dependency audit, and a Docker end-to-end run of the smoke test against PostgreSQL and RabbitMQ.

See [docs/VALIDATION.md](docs/VALIDATION.md) for the latest verified results.

## Troubleshooting

| Problem | Fix |
|---|---|
| `port is already allocated` or `Cannot bind port` | Another program, or the other run mode, is using port 5173, 8080 or 15672. Stop it, or run `./scripts/stop.ps1` / `docker compose down`. |
| `docker compose up --wait` reports a container as unhealthy | Run `docker compose ps` to find it, then `docker compose logs <service>`. On low-memory machines, give Docker Desktop at least 4 GB in *Settings → Resources*. |
| Cannot connect to the Docker daemon | Start Docker Desktop and wait until it reports that the engine is running. |
| `./mvnw: Permission denied` or `bad interpreter` (macOS/Linux) | Run `chmod +x mvnw`. If you copied the files from Windows, re-clone with Git so that `.gitattributes` keeps the LF line endings. |
| Login fails after changing `DEMO_PASSWORD` | Every service reads the password at startup. Recreate the containers with `docker compose up -d --force-recreate --wait`. |
| Changes to code don't appear in Docker | Rebuild with `docker compose up --build -d --wait`. |

## Architecture

```text
React dashboard (nginx :5173) → Spring Cloud Gateway :8080
                                  ├── Student Service      :8081 → student database
                                  ├── Recruitment Service  :8082 → recruitment database
                                  ├── Application Service  :8083 → application database + outbox
                                  └── Notification Service :8084 → notification database

Application Service → authenticated student/drive snapshot APIs
Outbox → RabbitMQ (Docker) or HTTP (native) → idempotent notification inbox
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

| | Docker | Native (Windows) |
|---|---|---|
| Databases | PostgreSQL, with a separate database and account per service | Four file-based H2 databases |
| Event transport | RabbitMQ | Authenticated HTTP |
| Schema | Flyway migrations | Flyway migrations |

Each service owns its database, and no service queries another service's tables. Eligibility is checked against the student and drive records read at submission time and stored with the application as a snapshot, so later profile changes do not alter past decisions. This version has no distributed lock between drive closure and application submission, so a submission that races a closure may be evaluated against the drive as it was just before closing.

What's implemented:

- Student, placement-officer and recruiter accounts, with authorization checked in each service.
- Student profiles and officer-controlled academic verification.
- Drive publishing and closing, branch filters, and company/role search.
- Server-side eligibility checks: verification, branch, batch, CGPA, backlogs, open status and deadline.
- Duplicate-application protection enforced by a database unique constraint.
- Ownership checks and a controlled application state machine.
- Interview scheduling with time and meeting link, an immutable eligibility snapshot, and status history.
- Application changes and their notification events committed together through a transactional outbox.
- Retried notification delivery, idempotent inbox writes and mark-as-read.
- Reports that distinguish offers from distinct students selected.

## Reliability behavior

Saving an application or a status change inserts its audit row and outbox row in the same transaction. The dispatcher retries delivery with capped exponential backoff. After eight failed attempts, the event is kept as `FAILED`. An officer can inspect `/api/outbox` and retry with `POST /api/outbox/retry`.

Notification IDs equal event IDs, so a repeated delivery does not create a second notification. In Docker, RabbitMQ uses durable queues, publisher confirms, mandatory returns, bounded consumer retries and a dead-letter queue (`campus.notifications.failed`). Events in the dead-letter queue need to be reviewed and requeued in RabbitMQ management after the underlying problem is fixed.

The outbox dispatcher supports **one Application Service instance**. Row leasing or locking is needed before running multiple replicas. Reports and list endpoints return the small demo dataset without server-side pagination.

## Security

This is a **development and demo configuration, not a production deployment**:

- Login uses HTTP Basic with in-memory demo accounts and stateless requests. There is no registration.
- The frontend keeps credentials in memory only and clears them on sign-out.
- Database, RabbitMQ and service passwords are fixed demo values in `compose.yaml`.
- All ports bind to loopback.

Before using real student data or exposing the system beyond your own computer, add OIDC login (for example Keycloak), real service credentials, HTTPS, secret management and an access review. Résumé upload and email delivery are not implemented yet. See [docs/ROADMAP.md](docs/ROADMAP.md) for the planned milestones.
