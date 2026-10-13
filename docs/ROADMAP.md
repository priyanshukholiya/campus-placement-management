# Milestone status

## Implemented in the first build

- Four business services and a Spring Cloud Gateway.
- React dashboards for students, officers and the demo recruiter.
- Drive creation, academic verification, eligibility and submission.
- Status workflow, interviews, audit snapshots and distinct-student reporting.
- Local persistent storage, PostgreSQL migrations, transactional outbox and idempotent notifications.
- Local HTTP event transport plus a RabbitMQ configuration for Docker.
- Unit, service integration and cross-service smoke tests.

## Next: identity and documents

1. Replace demo accounts with Keycloak Authorization Code + PKCE login.
2. Map JWT roles in every service; use scoped client credentials for internal calls.
3. Persist company/recruiter membership and officer-managed invitations.
4. Add résumé storage, file-type/size validation and authorized download access limited to an applicant's assigned recruiter and placement officers.

## Next: reporting and operational readiness

1. Server pagination, richer filters and CSV exports.
2. Outbox row leasing for multiple dispatcher instances and replay tooling for dead letters.
3. PostgreSQL/RabbitMQ integration tests using Testcontainers once Docker is running.
4. Correlation IDs, tracing, structured logs and service metrics.
5. OpenAPI generation and contract tests.
6. CI configuration after the owner explicitly authorizes GitHub linking.

The original ten-week plan remains the long-term scope; this initial working increment does not claim to complete every item in that plan.
