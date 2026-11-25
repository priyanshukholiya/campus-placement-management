# Validation — 22 November 2026

Environment: Windows 11, JDK 25.0.1, Node.js 22.20.0, Docker Desktop (Engine 29.9.3, Compose v5.7.0, 12 CPUs / 10 GB allocated).

## Docker (PostgreSQL + RabbitMQ)

- `docker compose up --build -d` built all images and started all 8 containers.
- From empty volumes, `docker compose up -d --wait` reported every container healthy after **45 seconds**, with no container restarts and no errors in the service logs.
- `node scripts/smoke.mjs` passed all **12** checks against the Docker gateway.
- Notifications went through RabbitMQ: 4 messages published and 4 delivered for one workflow run, with the dead-letter queue `campus.notifications.failed` empty.
- The nginx frontend served the app on `localhost:5173` and `127.0.0.1:5173` and passed `/api/*` through to the gateway.
- Persistence: after `docker compose down` and `up`, the earlier applications and outbox events were still in PostgreSQL and the smoke tests passed again.
- Startup ordering: every Java service, the gateway and the frontend have health checks. The gateway waits for all four services to be healthy, and the frontend waits for the gateway. The PostgreSQL check connects over TCP to the last database created by `infra/init-databases.sql`, so it cannot pass while first-time initialisation is still running.

## Native Windows mode (H2 + HTTP transport)

- `./mvnw verify`: all six modules built and the **12** backend tests passed (5 eligibility/state-machine, 6 application integration, 1 notification integration).
- Frontend `npm ci` and `npm run build` succeeded.
- `./scripts/start.ps1 -Smoke`: all five Java processes and Vite became healthy and the **12** smoke checks passed.
- Restart on existing data: after `stop.ps1` and `start.ps1 -Smoke`, the services reopened the existing H2 databases for writing and the smoke checks passed again. This resolves the read-only reopen issue recorded in the previous validation, which was specific to that sandbox.
- API calls through the Vite dev proxy on port 5173 returned HTTP 200.

## Cross-platform build

- `./mvnw -B verify` passed in a clean Linux container (`eclipse-temurin:21-jdk`), confirming the Unix Maven wrapper and the Java 21 minimum.
- `.gitattributes` forces LF line endings for `mvnw`, shell scripts, Dockerfiles and nginx config, so Windows clones with `core.autocrlf=true` still produce working files for Linux, macOS and containers.

## Functional spot check

- Seeded eligibility matches the README walkthrough: `student2` is ineligible for Aster Labs ("Eligible branches: CSE, IT.", "Minimum CGPA is 7.00.") and eligible for Northstar Systems.

## Not yet verified

- Browser interaction and rendered desktop/mobile layouts. These were checked by HTTP status only, not visually.
- Running on macOS hosts (Docker Desktop for Mac, including Apple Silicon). The images are built from multi-architecture base images but have not been run there.
- PostgreSQL/RabbitMQ integration tests inside the Maven build (Testcontainers). The Docker checks above are manual.

## Repeat

```bash
docker compose down -v                 # optional: start from empty volumes
docker compose up --build -d --wait
node scripts/smoke.mjs
```

Native Windows mode:

```powershell
./scripts/build.ps1
./scripts/start.ps1 -Smoke
./scripts/stop.ps1
```

The smoke tests create synthetic records in the current database. All records are fictional.
