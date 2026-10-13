# API map

The gateway is `http://127.0.0.1:8080`. Public routes require the demo account's HTTP Basic credentials. Errors normally return `code`, `message`, and `details`; security and framework-level errors may use standard HTTP error bodies.

| Method and path | Access | Purpose |
|---|---|---|
| `GET /api/session` | Signed-in user | Current username and roles |
| `GET /api/students/me` | Student | Own profile |
| `PUT /api/students/me` | Student | Update name and skills |
| `GET /api/students` | Officer | Student roster |
| `PATCH /api/students/{id}/academics` | Officer | Branch, batch, CGPA, backlogs and verification |
| `GET /api/drives` | Signed-in user | Placement drives |
| `GET /api/drives/{id}` | Signed-in user | One drive |
| `POST /api/drives` | Officer | Publish a new drive |
| `PATCH /api/drives/{id}/status` | Officer | `PUBLISHED` or `CLOSED` |
| `GET /api/applications/eligibility/{driveId}` | Student | Eligibility with specific reasons |
| `POST /api/applications` | Student | Submit `{ "driveId": "…" }` |
| `GET /api/applications` | Student / recruiter / officer | Applications visible to the caller |
| `PATCH /api/applications/{id}/status` | Owner student / assigned recruiter / officer | Valid status transition |
| `GET /api/applications/{id}/history` | Same ownership rules | Audit timeline |
| `GET /api/reports` | Recruiter / officer | Scoped counts and outcomes |
| `GET /api/outbox` | Officer | Pending, delivered, failed counts |
| `POST /api/outbox/retry` | Officer | Retry retained failed publisher events |
| `GET /api/notifications` | Signed-in user | Own inbox |
| `PATCH /api/notifications/{id}/read` | Notification owner | Mark read |

`/internal/students/{id}`, `/internal/drives/{id}` and `POST /internal/notifications` exist on their business services, require the separate `service` account, and are not routed through the gateway.

## Status transitions

```text
SUBMITTED → SHORTLISTED → INTERVIEW_SCHEDULED → SELECTED
     └───────────┴────────────────┴──────────→ REJECTED
     └───────────┴────────────────┴──────────→ WITHDRAWN
```

Students can request only `WITHDRAWN` for their own active applications. All three terminal states are final. This version does not permit reapplication after withdrawal.

An interview transition requires `interviewAt` as a future ISO-8601 instant and `meetingUrl` as an HTTP(S) URL. Other transitions accept an optional `note` up to 500 characters. Date inputs in the UI use the browser's local timezone and are sent to the server as UTC instants.

## Drive creation example

```json
{
  "company": "Example Labs",
  "title": "Graduate Java Engineer",
  "location": "Bengaluru",
  "description": "Build and test Java services with a product team.",
  "recruiterUsername": "recruiter",
  "branches": ["CSE", "IT"],
  "graduationYear": 2027,
  "minCgpa": 7.0,
  "maxBacklogs": 0,
  "packageLpa": 8.5,
  "deadline": "2027-01-01T12:00:00Z"
}
```

Choose an actual future deadline. The fixed `recruiter` assignment corresponds to the one recruiter account supported in this demo.
