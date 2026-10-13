# Validation — 13 October 2026

## Passed

- Maven reactor build for all six modules, targeting Java 21, using JDK 25.0.1 and Maven 3.9.11.
- 12 backend tests: 5 eligibility/state-machine tests, 6 application integration tests, and 1 notification integration test. No failures or skipped tests. The workflow test also verifies that missing URL schemes and non-HTTP interview links return HTTP 400 without changing application history or notification events.
- All five Java processes and Vite started, and all health checks returned HTTP 200.
- 12 cross-service smoke checks against the real gateway and services: authentication, drive publication, eligibility, ineligible submission rejection, successful application and duplicate rejection, ownership and role checks, invalid transition rejection, full recruitment progression and four audit entries, reporting, outbox delivery, private inbox actions, and drive closure.
- React production build.
- Frontend dependency audit: zero reported vulnerabilities at validation time.
- Docker Compose configuration parsing.
- Maven wrapper resolves Maven 3.9.11 when its download temporary directory is inside the workspace. The build script now uses a project-local temporary directory automatically.

The smoke run used fresh file-backed H2 databases and the authenticated HTTP notification transport. All records are synthetic.

## Not yet verified

- Docker containers, PostgreSQL runtime behavior, and RabbitMQ delivery: Docker Desktop's Linux engine was unavailable. The Compose file is syntax-checked only.
- Browser interactions and rendered desktop/mobile layouts: the installed Edge browser could not start successfully inside the Windows sandbox, reporting access failures in its temporary profile. Frontend compilation is verified; visual QA is not.
- Restarting existing H2 databases for further writes inside this sandbox: Java's `Files.isWritable` returned false even though `File.canWrite` and a read/write file open succeeded. H2 therefore reopened an existing database read-only. The full workflow passed with freshly initialized files. Persistence across ordinary PowerShell restarts outside the sandbox remains to be checked; no filesystem permissions were changed to bypass the issue.

## Repeat locally

From the project folder in a normal PowerShell session:

```powershell
./scripts/build.ps1 -JavaHome 'C:/Program Files/Java/jdk-25'
./scripts/start.ps1 -JavaHome 'C:/Program Files/Java/jdk-25' -Smoke
```

Open `http://127.0.0.1:5173`, sign in as `student`, `officer`, or `recruiter` with password `placement-demo`, and follow the workflow in the README. Then run `./scripts/stop.ps1`, restart, and confirm previous records remain editable.

Do not delete existing databases as a routine fix for permission errors. Preserve them and check how the process was launched and whether another instance is using them.
