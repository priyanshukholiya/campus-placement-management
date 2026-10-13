import assert from "node:assert/strict";
const base = process.env.CAMPUS_URL || "http://127.0.0.1:8080";
const password = process.env.DEMO_PASSWORD || "placement-demo";
const checks = [];
async function call(user, method, path, body, expected = 200) {
  const response = await fetch(base + "/api" + path, {
    method,
    headers: {
      Authorization:
        "Basic " + Buffer.from(user + ":" + password).toString("base64"),
      "Content-Type": "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(15000),
  });
  const text = await response.text();
  assert.equal(response.status, expected, `${method} ${path}: ${text}`);
  return text ? JSON.parse(text) : null;
}
function pass(message) {
  checks.push(message);
  console.log("PASS " + message);
}
await call("student", "GET", "/session");
pass("Gateway authentication");
const drive = await call(
  "officer",
  "POST",
  "/drives",
  {
    company: "Smoke Test Labs",
    title: "Java Graduate Engineer",
    location: "Local test",
    description: "Synthetic cross-service test record.",
    recruiterUsername: "recruiter",
    branches: ["CSE"],
    graduationYear: 2027,
    minCgpa: 7,
    maxBacklogs: 0,
    packageLpa: 8.5,
    deadline: new Date(Date.now() + 86400000 * 7).toISOString(),
  },
  201,
);
pass("Officer publishes a drive");
const eligible = await call(
  "student",
  "GET",
  "/applications/eligibility/" + drive.id,
);
assert.equal(eligible.eligible, true);
const ineligible = await call(
  "student2",
  "GET",
  "/applications/eligibility/" + drive.id,
);
assert.equal(ineligible.eligible, false);
assert.ok(ineligible.reasons.length >= 2);
pass("Eligibility is computed from real student and drive services");
await call("student2", "POST", "/applications", { driveId: drive.id }, 422);
pass("Ineligible submission rejected");
const application = await call(
  "student",
  "POST",
  "/applications",
  { driveId: drive.id },
  201,
);
await call("student", "POST", "/applications", { driveId: drive.id }, 409);
pass("Application persists and duplicate submission is rejected");
await call(
  "student2",
  "GET",
  `/applications/${application.id}/history`,
  undefined,
  404,
);
await call("student", "GET", "/students", undefined, 403);
pass("Ownership and officer-only APIs protected");
await call(
  "officer",
  "PATCH",
  `/applications/${application.id}/status`,
  { status: "SELECTED" },
  409,
);
pass("Cannot skip recruitment stages");
await call("recruiter", "PATCH", `/applications/${application.id}/status`, {
  status: "SHORTLISTED",
  note: "Smoke test shortlist",
});
await call("officer", "PATCH", `/applications/${application.id}/status`, {
  status: "INTERVIEW_SCHEDULED",
  interviewAt: new Date(Date.now() + 86400000).toISOString(),
  meetingUrl: "https://example.com/interview",
  note: "Synthetic interview",
});
await call("officer", "PATCH", `/applications/${application.id}/status`, {
  status: "SELECTED",
  note: "Smoke test complete",
});
const history = await call(
  "student",
  "GET",
  `/applications/${application.id}/history`,
);
assert.equal(history.length, 4);
pass("Full recruitment workflow and audit history");
const report = await call("officer", "GET", "/reports");
assert.ok(report.placedStudents >= 1);
assert.ok(report.offers >= report.placedStudents);
pass("Distinct-student placement report");
let notifications = [];
for (let i = 0; i < 15; i++) {
  notifications = await call("student", "GET", "/notifications");
  if (
    notifications.filter((n) => n.message.includes("Smoke Test Labs")).length >=
    4
  )
    break;
  await new Promise((r) => setTimeout(r, 2000));
}
assert.ok(
  notifications.filter((n) => n.message.includes("Smoke Test Labs")).length >=
    4,
);
pass("Outbox delivers all status notifications");
const target = notifications.find((n) => n.message.includes("Smoke Test Labs"));
await call(
  "student2",
  "PATCH",
  `/notifications/${target.id}/read`,
  undefined,
  404,
);
await call("student", "PATCH", `/notifications/${target.id}/read`);
pass("Private inbox ownership and mark-as-read");
await call("officer", "PATCH", `/drives/${drive.id}/status`, {
  status: "CLOSED",
});
pass("Officer closes a drive");
console.log(
  `\n${checks.length} cross-service checks passed. Synthetic records remain for inspection.`,
);
