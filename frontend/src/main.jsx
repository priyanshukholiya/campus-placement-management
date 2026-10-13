import React, { useEffect, useState, useRef } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";

const iconPaths = {
  grid: "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z",
  briefcase: "M8 6V3h8v3 M3 6h18v14H3z M3 11c6 4 12 4 18 0 M10 12h4",
  file: "M6 3h8l4 4v14H6z M14 3v5h4 M9 12h6 M9 16h6",
  users:
    "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M20 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75",
  bell: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9 M10 21h4",
  arrow: "M5 12h14 M14 7l5 5-5 5",
  plus: "M12 5v14 M5 12h14",
  search: "M21 21l-5-5 M10 3a7 7 0 1 0 0 14 7 7 0 0 0 0-14",
  check: "M5 12l4 4L19 6",
  clock: "M12 8v4l3 2 M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18",
  pin: "M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0 M12 7a3 3 0 1 0 0 6 3 3 0 0 0 0-6",
  logout: "M9 3H3v18h6 M9 12h12 M17 8l4 4-4 4",
  close: "M6 6l12 12 M6 18L18 6",
  trend: "M3 17l6-6 4 4 8-10 M15 5h6v6",
  shield: "M12 3l8 3v6c0 5-8 9-8 9s-8-4-8-9V6z M8 12l3 3 5-6",
  refresh: "M20 7v5h-5 M4 17v-5h5 M6 6a8 8 0 0 1 14 6 M18 18A8 8 0 0 1 4 12",
  book: "M12 5c-4-3-8-2-10-1v15c3-2 7-2 10 0 3-2 7-2 10 0V4c-2-1-6-2-10 1z M12 5v14",
};
function Icon({ name, size = 20, ...props }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      <path d={iconPaths[name] || iconPaths.file} />
    </svg>
  );
}
const label = (s) =>
  s
    ?.toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (c) => c.toUpperCase());
const date = (v) =>
  new Date(v).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
const dateTime = (v) =>
  new Date(v).toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
const active = (d) =>
  d.status === "PUBLISHED" && new Date(d.deadline) > new Date();
const initials = (name) =>
  name
    ?.split(" ")
    .slice(0, 2)
    .map((s) => s[0])
    .join("") || "C";
function Badge({ status }) {
  return (
    <span className={"badge " + status.toLowerCase()}>{label(status)}</span>
  );
}
function Modal({ title, children, onClose }) {
  useEffect(() => {
    const close = (e) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", close);
    const before = document.activeElement;
    const focus = () =>
      document.querySelector(".modal input, .modal button")?.focus();
    requestAnimationFrame(focus);
    return () => {
      window.removeEventListener("keydown", close);
      before?.focus?.();
    };
  }, []);
  return (
    <div className="overlay" onClick={onClose}>
      <section
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
        onKeyDown={(e) => {
          if (e.key !== "Tab") return;
          const nodes = [
            ...e.currentTarget.querySelectorAll(
              "button:not(:disabled),input,select,textarea,a[href]",
            ),
          ];
          const first = nodes[0],
            last = nodes.at(-1);
          if (e.shiftKey && document.activeElement === first) {
            e.preventDefault();
            last?.focus();
          } else if (!e.shiftKey && document.activeElement === last) {
            e.preventDefault();
            first?.focus();
          }
        }}
      >
        <div className="modal-head">
          <h2>{title}</h2>
          <button
            className="icon-button"
            onClick={onClose}
            aria-label="Close dialog"
          >
            <Icon name="close" />
          </button>
        </div>
        {children}
      </section>
    </div>
  );
}

function App() {
  const activeAuth = useRef(""),
    refreshVersion = useRef(0);
  const [session, setSession] = useState(null),
    [credentials, setCredentials] = useState(""),
    [username, setUsername] = useState("student"),
    [password, setPassword] = useState("placement-demo");
  const [page, setPage] = useState("overview"),
    [drives, setDrives] = useState([]),
    [applications, setApplications] = useState([]),
    [students, setStudents] = useState([]),
    [profile, setProfile] = useState(null),
    [notifications, setNotifications] = useState([]),
    [report, setReport] = useState(null),
    [outbox, setOutbox] = useState(null);
  const [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(false),
    [query, setQuery] = useState(""),
    [branch, setBranch] = useState("ALL"),
    [modal, setModal] = useState(null);
  const role = session?.roles[0],
    isStudent = role === "STUDENT",
    isOfficer = role === "OFFICER";
  async function api(url, options = {}, auth = credentials) {
    const res = await fetch("/api" + url, {
      ...options,
      headers: {
        Authorization: "Basic " + auth,
        "Content-Type": "application/json",
        ...options.headers,
      },
    });
    const body = res.status === 204 ? null : await res.json().catch(() => null);
    if (!res.ok)
      throw new Error(
        [
          body?.message || `Request failed (${res.status}).`,
          ...(body?.details || []),
        ].join(" "),
      );
    return body;
  }
  async function refresh(auth = credentials, user = session, quiet = false) {
    if (!user) return;
    const version = ++refreshVersion.current;
    const current = () =>
      activeAuth.current === auth && refreshVersion.current === version;
    if (!quiet) setLoading(true);
    try {
      const r = user.roles[0];
      const [d, a, n, s, p, o] = await Promise.all([
        api("/drives", {}, auth),
        api("/applications", {}, auth),
        api("/notifications", {}, auth),
        r === "STUDENT"
          ? api("/students/me", {}, auth)
          : r === "OFFICER"
            ? api("/students", {}, auth)
            : Promise.resolve(null),
        r !== "STUDENT" ? api("/reports", {}, auth) : Promise.resolve(null),
        r === "OFFICER" ? api("/outbox", {}, auth) : Promise.resolve(null),
      ]);
      if (!current()) return;
      setDrives(d);
      setApplications(a);
      setNotifications(n);
      if (r === "STUDENT") setProfile(s);
      else setStudents(s || []);
      setReport(p);
      setOutbox(o);
    } catch (e) {
      if (current()) setError(e.message);
    } finally {
      if (current()) setLoading(false);
    }
  }
  useEffect(() => {
    if (!session) return;
    const t = setInterval(() => refresh(credentials, session, true), 12000);
    return () => clearInterval(t);
  }, [session, credentials]);
  useEffect(() => {
    if (!notice) return;
    const t = setTimeout(() => setNotice(""), 4500);
    return () => clearTimeout(t);
  }, [notice]);
  async function login(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const auth = btoa(username + ":" + password),
        user = await api("/session", {}, auth);
      activeAuth.current = auth;
      setCredentials(auth);
      setSession(user);
      setPage("overview");
      await refresh(auth, user);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  function logout() {
    activeAuth.current = "";
    refreshVersion.current++;
    setSession(null);
    setCredentials("");
    setProfile(null);
    setApplications([]);
    setDrives([]);
    setStudents([]);
    setNotifications([]);
    setReport(null);
    setOutbox(null);
    setLoading(false);
    setModal(null);
    setError("");
    setNotice("");
  }
  async function mutate(task, message) {
    setBusy(true);
    setError("");
    try {
      await task();
      setModal(null);
      setNotice(message);
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function checkEligibility(drive) {
    setError("");
    setBusy(true);
    try {
      const eligibility = await api("/applications/eligibility/" + drive.id);
      setModal({ type: "apply", drive, eligibility });
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function details(application) {
    setError("");
    try {
      const history = await api("/applications/" + application.id + "/history");
      setModal({ type: "application", application, history });
    } catch (e) {
      setError(e.message);
    }
  }
  function navigate(p) {
    setPage(p);
    setQuery("");
    setBranch("ALL");
    setError("");
  }
  const unread = notifications.filter((n) => !n.readAt).length;
  const openDrives = drives.filter(active);
  const selected = applications.filter((a) => a.status === "SELECTED");
  const displayName = isStudent
    ? profile?.name
    : role === "OFFICER"
      ? "Placement officer"
      : "Recruitment team";
  const nav = [
    ["overview", "grid", "Overview"],
    ["drives", "briefcase", "Placement drives"],
    ["applications", "file", "Applications"],
    ...(isOfficer ? [["students", "users", "Students"]] : []),
    ...(isStudent ? [["profile", "users", "My profile"]] : []),
    ["notifications", "bell", "Notifications"],
  ];
  if (!session)
    return (
      <div className="login-layout">
        <div className="login-story">
          <div className="brand">
            <span className="brand-symbol">
              <Icon name="book" size={25} />
            </span>
            <span>
              campus<span className="brand-dot">.</span>
            </span>
          </div>
          <div className="story-content">
            <span className="eyebrow light">YOUR NEXT CHAPTER STARTS HERE</span>
            <h1>
              Great futures.
              <br />
              One campus.
            </h1>
            <p>
              A shared space for students, placement teams, and the
              opportunities that bring them together.
            </p>
            <div className="story-art">
              <div className="art-line" />
              <div className="art-node first">
                <Icon name="book" size={30} />
                <span>Learn</span>
              </div>
              <div className="art-node second">
                <Icon name="users" size={30} />
                <span>Connect</span>
              </div>
              <div className="art-node third">
                <Icon name="trend" size={30} />
                <span>Grow</span>
              </div>
            </div>
          </div>
          <div className="story-footer">
            CAMPUS PLACEMENT MANAGEMENT <span>CLASS OF 2027</span>
          </div>
        </div>
        <main className="login-panel">
          <span className="small-tag">
            <span className="dot" /> Local development demo
          </span>
          <h2>
            Welcome to your
            <br />
            placement workspace.
          </h2>
          <p className="muted">
            Choose a demo account to explore the complete flow.
          </p>
          <div className="persona-options">
            {[
              ["student", "Student"],
              ["officer", "Officer"],
              ["recruiter", "Recruiter"],
            ].map(([id, name]) => (
              <button
                key={id}
                className={username === id ? "chosen" : ""}
                onClick={() => setUsername(id)}
              >
                {name}
              </button>
            ))}
          </div>
          <form onSubmit={login}>
            <label>
              Username
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
            </label>
            {error && (
              <div role="alert" className="error">
                {error}
              </div>
            )}
            <button className="primary full" disabled={busy}>
              {busy ? "Connecting…" : "Enter workspace"}
              <Icon name="arrow" />
            </button>
          </form>
          <p className="demo-note">
            Demo password: <code>placement-demo</code>
            <br />
            Try <code>student2</code> to see a different eligibility result.
            <br />
            All student and company records are fictional.
          </p>
        </main>
      </div>
    );
  function DriveCard({ drive: d, compact = false }) {
    const applied = applications.some(
      (a) => a.driveId === d.id && a.studentId === session.username,
    );
    return (
      <article className={"drive-card " + (compact ? "compact" : "")}>
        <div className="drive-top">
          <div className={"company-icon company-" + (d.company.length % 3)}>
            {initials(d.company)}
          </div>
          <div>
            <span className="company-name">{d.company}</span>
            <h3>{d.title}</h3>
          </div>
          <span className={"status-dot " + (active(d) ? "open" : "")}>
            {active(d) ? "Open" : "Closed"}
          </span>
        </div>
        <div className="drive-location">
          <Icon name="pin" size={15} />
          {d.location}
        </div>
        {!compact && <p className="drive-description">{d.description}</p>}
        <div className="drive-chips">
          <span>{d.branches.replaceAll(",", " · ")}</span>
          <span>{Number(d.minCgpa).toFixed(1)}+ CGPA</span>
          <span>Batch {d.graduationYear}</span>
        </div>
        <div className="drive-bottom">
          <div>
            <strong>
              ₹{Number(d.packageLpa).toFixed(1)} <small>LPA</small>
            </strong>
            <span className="deadline">
              <Icon name="clock" size={13} />
              Apply by {date(d.deadline)}
            </span>
          </div>
          {isStudent ? (
            <button
              className={applied ? "secondary" : "text-button"}
              disabled={busy || applied || !active(d)}
              onClick={() => checkEligibility(d)}
            >
              {applied ? "Applied" : "View opportunity"}
              {!applied && <Icon name="arrow" size={17} />}
            </button>
          ) : isOfficer ? (
            <button
              className="text-button"
              disabled={
                busy || (!active(d) && new Date(d.deadline) <= new Date())
              }
              onClick={() =>
                mutate(
                  () =>
                    api("/drives/" + d.id + "/status", {
                      method: "PATCH",
                      body: JSON.stringify({
                        status:
                          d.status === "PUBLISHED" ? "CLOSED" : "PUBLISHED",
                      }),
                    }),
                  "Drive updated.",
                )
              }
            >
              {d.status === "PUBLISHED" ? "Close drive" : "Reopen drive"}
            </button>
          ) : (
            <button
              className="text-button"
              onClick={() => navigate("applications")}
            >
              Review applicants
              <Icon name="arrow" size={17} />
            </button>
          )}
        </div>
      </article>
    );
  }
  function ApplicationsTable({ rows = applications }) {
    return rows.length ? (
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              {!isStudent && <th>Student</th>}
              <th>Opportunity</th>
              <th>Status</th>
              <th>Applied</th>
              <th>
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {rows.map((a) => (
              <tr key={a.id}>
                {!isStudent && (
                  <td>
                    <strong>{a.studentName}</strong>
                    <small>
                      {a.branch} · {Number(a.cgpa).toFixed(2)} CGPA
                    </small>
                  </td>
                )}
                <td>
                  <strong>{a.company}</strong>
                  <small>{a.driveTitle}</small>
                </td>
                <td>
                  <Badge status={a.status} />
                </td>
                <td>{date(a.appliedAt)}</td>
                <td>
                  <button className="text-button" onClick={() => details(a)}>
                    View
                    <Icon name="arrow" size={16} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ) : (
      <Empty
        icon="file"
        title="A fresh start"
        text={
          isStudent
            ? "Your applications will appear here after you apply to a placement drive."
            : "Submitted student applications will appear here for review."
        }
      />
    );
  }
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-symbol">
            <Icon name="book" size={23} />
          </span>
          <span>
            campus<span className="brand-dot">.</span>
          </span>
        </div>
        <div className="workspace-label">PLACEMENT WORKSPACE</div>
        <nav>
          {nav.map(([id, icon, name]) => (
            <button
              key={id}
              aria-label={name}
              className={page === id ? "active" : ""}
              onClick={() => navigate(id)}
            >
              <Icon name={icon} />
              <span>{name}</span>
              {id === "notifications" && unread > 0 && (
                <span className="nav-count">{unread}</span>
              )}
            </button>
          ))}
        </nav>
        <div className="sidebar-callout">
          <Icon name="shield" size={23} />
          <strong>
            Every next step,
            <br />
            in one place.
          </strong>
          <p>
            Keep your profile ready.
            <br />
            Your opportunity is ahead.
          </p>
          <span>2027 PLACEMENT SEASON</span>
        </div>
        <div className="account">
          <span className="avatar">{initials(displayName)}</span>
          <div>
            <strong>{displayName}</strong>
            <small>{label(role)} account</small>
          </div>
          <button
            onClick={logout}
            disabled={busy}
            className="icon-button"
            aria-label="Sign out"
          >
            <Icon name="logout" size={18} />
          </button>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <div className="breadcrumb">
            Workspace <span>/</span>{" "}
            <strong>{nav.find((n) => n[0] === page)?.[2]}</strong>
          </div>
          <div className="top-actions">
            <span className="today">{date(new Date())}</span>
            <button
              className="icon-button"
              title="Refresh data"
              aria-label="Refresh data"
              disabled={loading}
              onClick={() => refresh()}
            >
              <Icon name="refresh" />
            </button>
            <button
              className="icon-button notification-button"
              aria-label="Open notifications"
              onClick={() => navigate("notifications")}
            >
              <Icon name="bell" />
              {unread > 0 && <i />}
            </button>
            <span className="avatar small">{initials(displayName)}</span>
          </div>
        </header>
        <main className="content">
          <div className="page-title">
            <div>
              <div className="eyebrow">
                {isStudent ? "YOUR CAREER, IN MOTION" : "CAMPUS TO CAREER"}
              </div>
              <h1>
                {page === "overview"
                  ? isStudent
                    ? `Good to see you, ${profile?.name.split(" ")[0] || "there"}.`
                    : "Your placement overview."
                  : nav.find((n) => n[0] === page)?.[2]}
              </h1>
              <p>
                {
                  {
                    overview: isStudent
                      ? "A little preparation. A new opportunity. Your next chapter."
                      : "A clear view of your drives, candidates, and placement outcomes.",
                    drives:
                      "Explore opportunities and find the right next step.",
                    applications:
                      "Follow every application, from the first click to the final offer.",
                    students:
                      "Review academic details and keep eligibility decisions accurate.",
                    profile:
                      "Keep your details current and put your best foot forward.",
                    notifications: "The latest updates, all in one place.",
                  }[page]
                }
              </p>
            </div>
            {isOfficer && ["overview", "drives"].includes(page) && (
              <button
                className="primary"
                onClick={() => setModal({ type: "drive" })}
              >
                <Icon name="plus" size={18} />
                Create drive
              </button>
            )}
          </div>
          {error && (
            <div className="error global-error" role="alert">
              {error}
              <button
                className="icon-button"
                aria-label="Dismiss error"
                onClick={() => setError("")}
              >
                <Icon name="close" size={17} />
              </button>
            </div>
          )}
          {notice && (
            <div className="success" role="status">
              <Icon name="check" size={17} />
              {notice}
            </div>
          )}
          {loading && (
            <div className="loading" role="status">
              Updating your workspace…
            </div>
          )}
          {page === "overview" && (
            <>
              <div className="stats-grid">
                {[
                  [
                    isStudent ? "Open opportunities" : "Active drives",
                    openDrives.length,
                    "briefcase",
                    "Explore what’s next",
                  ],
                  [
                    "Applications",
                    applications.length,
                    "file",
                    isStudent
                      ? "Your journey so far"
                      : "Across your recruitment drives",
                  ],
                  [
                    isStudent ? "Interviews scheduled" : "Students placed",
                    isStudent
                      ? applications.filter(
                          (a) => a.status === "INTERVIEW_SCHEDULED",
                        ).length
                      : report?.placedStudents || 0,
                    "users",
                    isStudent
                      ? "Time to make an impression"
                      : "Distinct students selected",
                  ],
                  [
                    isStudent ? "Offers received" : "Offers made",
                    isStudent ? selected.length : report?.offers || 0,
                    "trend",
                    "Every milestone matters",
                  ],
                ].map(([title, value, icon, note]) => (
                  <div className="stat-card" key={title}>
                    <div className="stat-top">
                      <span>{title}</span>
                      <span className="stat-icon">
                        <Icon name={icon} />
                      </span>
                    </div>
                    <strong>{value}</strong>
                    <small>{note}</small>
                  </div>
                ))}
              </div>
              <div className="overview-columns">
                <div>
                  <section className="section-head">
                    <div>
                      <h2>Open for your next move</h2>
                      <p>Current opportunities on campus</p>
                    </div>
                    <button
                      className="text-button"
                      onClick={() => navigate("drives")}
                    >
                      View all
                      <Icon name="arrow" size={17} />
                    </button>
                  </section>
                  <div className="drive-stack">
                    {openDrives.slice(0, 2).map((d) => (
                      <DriveCard key={d.id} drive={d} compact />
                    ))}
                    {!openDrives.length && (
                      <Empty
                        icon="briefcase"
                        title="New opportunities are on the way"
                        text="Published drives with upcoming deadlines will appear here."
                      />
                    )}
                  </div>
                </div>
                <aside className="overview-aside">
                  <div className="season-card">
                    <span className="eyebrow light">THE NEXT CHAPTER</span>
                    <h2>
                      Opportunity meets
                      <br />
                      preparation.
                    </h2>
                    <p>
                      {isStudent
                        ? "A verified profile helps you apply with confidence."
                        : "Turn strong potential into a promising career."}
                    </p>
                    <div className="season-illustration">
                      <span>20</span>
                      <span>
                        27
                        <Icon name="arrow" size={47} />
                      </span>
                    </div>
                    <button
                      onClick={() =>
                        navigate(
                          isStudent
                            ? "profile"
                            : isOfficer
                              ? "students"
                              : "applications",
                        )
                      }
                      className="light-button"
                    >
                      {isStudent
                        ? "Review your profile"
                        : isOfficer
                          ? "View student profiles"
                          : "Review applications"}
                      <Icon name="arrow" size={16} />
                    </button>
                  </div>
                  <div className="checklist">
                    <h3>
                      {isStudent
                        ? "Your application checklist"
                        : "Placement pulse"}
                    </h3>
                    {isStudent ? (
                      <>
                        <div>
                          <Icon name={profile?.verified ? "check" : "clock"} />
                          <span>
                            Academic profile{" "}
                            {profile?.verified
                              ? "verified"
                              : "awaiting verification"}
                          </span>
                        </div>
                        <div>
                          <Icon name={profile?.skills ? "check" : "clock"} />
                          <span>
                            Skills{" "}
                            {profile?.skills
                              ? "added to your profile"
                              : "ready to add"}
                          </span>
                        </div>
                        <div>
                          <Icon name="briefcase" />
                          <span>
                            {openDrives.length} opportunities to explore
                          </span>
                        </div>
                      </>
                    ) : (
                      <>
                        <div>
                          <Icon name="briefcase" />
                          <span>
                            {openDrives.length} drives accepting applications
                          </span>
                        </div>
                        <div>
                          <Icon name="users" />
                          <span>
                            {
                              applications.filter(
                                (a) => a.status === "SUBMITTED",
                              ).length
                            }{" "}
                            applications awaiting review
                          </span>
                        </div>
                        {outbox && (
                          <div>
                            <Icon name="bell" />
                            <span>
                              {outbox.pending} notification events pending
                            </span>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </aside>
              </div>
              <section className="panel applications-panel">
                <div className="section-head">
                  <div>
                    <h2>
                      {isStudent
                        ? "Your recent applications"
                        : "Recent applications"}
                    </h2>
                    <p>Keep track of every step</p>
                  </div>
                  <button
                    className="text-button"
                    onClick={() => navigate("applications")}
                  >
                    View all
                    <Icon name="arrow" size={17} />
                  </button>
                </div>
                <ApplicationsTable rows={applications.slice(0, 5)} />
              </section>
            </>
          )}
          {page === "drives" && (
            <>
              <div className="filters">
                <div className="search-box">
                  <Icon name="search" />
                  <input
                    aria-label="Search opportunities"
                    placeholder="Search company, role, or location…"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                </div>
                <select
                  aria-label="Filter by branch"
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                >
                  <option value="ALL">All branches</option>
                  {["CSE", "IT", "ECE", "EE", "ME", "CE"].map((b) => (
                    <option key={b}>{b}</option>
                  ))}
                </select>
                <span>
                  {
                    drives.filter(
                      (d) =>
                        (d.company + " " + d.title + " " + d.location)
                          .toLowerCase()
                          .includes(query.toLowerCase()) &&
                        (branch === "ALL" ||
                          d.branches.split(",").includes(branch)),
                    ).length
                  }{" "}
                  opportunities
                </span>
              </div>
              <div className="drives-grid">
                {drives
                  .filter(
                    (d) =>
                      (d.company + " " + d.title + " " + d.location)
                        .toLowerCase()
                        .includes(query.toLowerCase()) &&
                      (branch === "ALL" ||
                        d.branches.split(",").includes(branch)),
                  )
                  .map((d) => (
                    <DriveCard key={d.id} drive={d} />
                  ))}
              </div>
              {!drives.some(
                (d) =>
                  (d.company + " " + d.title + " " + d.location)
                    .toLowerCase()
                    .includes(query.toLowerCase()) &&
                  (branch === "ALL" || d.branches.split(",").includes(branch)),
              ) && (
                <Empty
                  icon="search"
                  title="No matching opportunities"
                  text="Try another company, role, or branch."
                />
              )}
            </>
          )}
          {page === "applications" && (
            <section className="panel">
              <div className="filters">
                <div className="search-box">
                  <Icon name="search" />
                  <input
                    aria-label="Search applications"
                    placeholder="Search applications…"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                </div>
              </div>
              <ApplicationsTable
                rows={applications.filter((a) =>
                  (
                    a.company +
                    " " +
                    a.driveTitle +
                    " " +
                    a.studentName +
                    " " +
                    a.status
                  )
                    .toLowerCase()
                    .includes(query.toLowerCase()),
                )}
              />
            </section>
          )}
          {page === "students" && isOfficer && (
            <section className="panel">
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Student</th>
                      <th>Branch / batch</th>
                      <th>CGPA</th>
                      <th>Backlogs</th>
                      <th>Verification</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {students.map((s) => (
                      <tr key={s.id}>
                        <td>
                          <strong>{s.name}</strong>
                          <small>{s.id}</small>
                        </td>
                        <td>
                          {s.branch} · {s.graduationYear}
                        </td>
                        <td>{Number(s.cgpa).toFixed(2)}</td>
                        <td>{s.activeBacklogs}</td>
                        <td>
                          <Badge status={s.verified ? "VERIFIED" : "PENDING"} />
                        </td>
                        <td>
                          <button
                            className="text-button"
                            onClick={() =>
                              setModal({ type: "academics", student: s })
                            }
                          >
                            Review
                            <Icon name="arrow" size={16} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}
          {page === "profile" && profile && (
            <div className="profile-grid">
              <section className="panel padded">
                <h2>Your profile</h2>
                <p className="muted">
                  Add the skills you want your next opportunity to use.
                </p>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    const f = new FormData(e.currentTarget);
                    mutate(
                      () =>
                        api("/students/me", {
                          method: "PUT",
                          body: JSON.stringify(Object.fromEntries(f)),
                        }),
                      "Profile saved.",
                    );
                  }}
                >
                  <label>
                    Full name
                    <input
                      name="name"
                      defaultValue={profile.name}
                      required
                      maxLength={120}
                    />
                  </label>
                  <label>
                    Skills
                    <textarea
                      name="skills"
                      defaultValue={profile.skills}
                      maxLength={1000}
                      rows={4}
                    />
                  </label>
                  <button className="primary" disabled={busy}>
                    Save profile
                    <Icon name="check" size={17} />
                  </button>
                </form>
              </section>
              <section className="panel padded">
                <h2>Academic record</h2>
                <p className="muted">Managed by your placement officer.</p>
                <dl className="detail-list">
                  <dt>Branch</dt>
                  <dd>{profile.branch}</dd>
                  <dt>Graduating batch</dt>
                  <dd>{profile.graduationYear}</dd>
                  <dt>CGPA</dt>
                  <dd>{Number(profile.cgpa).toFixed(2)} / 10</dd>
                  <dt>Active backlogs</dt>
                  <dd>{profile.activeBacklogs}</dd>
                  <dt>Verification</dt>
                  <dd>
                    <Badge status={profile.verified ? "VERIFIED" : "PENDING"} />
                  </dd>
                </dl>
              </section>
            </div>
          )}
          {page === "notifications" && (
            <section className="panel">
              {notifications.length ? (
                notifications.map((n) => (
                  <article
                    key={n.id}
                    className={
                      "notification-row " + (!n.readAt ? "unread" : "")
                    }
                  >
                    <span className="stat-icon">
                      <Icon name="bell" />
                    </span>
                    <div>
                      <h3>{n.title}</h3>
                      <p>{n.message}</p>
                      <small>{dateTime(n.occurredAt)}</small>
                    </div>
                    {!n.readAt && (
                      <button
                        className="text-button"
                        disabled={busy}
                        onClick={() =>
                          mutate(
                            () =>
                              api("/notifications/" + n.id + "/read", {
                                method: "PATCH",
                              }),
                            "Notification marked as read.",
                          )
                        }
                      >
                        Mark read
                      </button>
                    )}
                  </article>
                ))
              ) : (
                <Empty
                  icon="bell"
                  title="You're all caught up"
                  text="Application updates will arrive here automatically."
                />
              )}
            </section>
          )}
          <footer className="page-footer">
            <span>Campus placement workspace</span>
            <span className="footer-status">
              <span className="dot" /> Local demo · Synthetic data
            </span>
          </footer>
        </main>
      </div>
      {modal && (
        <Modal
          title={
            {
              drive: "Publish a placement drive",
              apply: "Your next opportunity",
              application: "Application details",
              academics: "Review academic record",
              status: "Update application",
            }[modal.type]
          }
          onClose={() => {
            if (!busy) setModal(null);
          }}
        >
          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}
          {modal.type === "apply" && (
            <>
              <div className="modal-opportunity">
                <span className="company-name">{modal.drive.company}</span>
                <h2>{modal.drive.title}</h2>
                <p>{modal.drive.description}</p>
              </div>
              <dl className="detail-list">
                <dt>Location</dt>
                <dd>{modal.drive.location}</dd>
                <dt>Package</dt>
                <dd>₹{modal.drive.packageLpa} LPA</dd>
                <dt>Deadline</dt>
                <dd>{dateTime(modal.drive.deadline)}</dd>
                <dt>Backlogs allowed</dt>
                <dd>{modal.drive.maxBacklogs}</dd>
              </dl>
              <div className={modal.eligibility.eligible ? "success" : "error"}>
                {modal.eligibility.eligible ? (
                  <>
                    <Icon name="check" />
                    You meet the eligibility requirements.
                  </>
                ) : (
                  <div>
                    <strong>You’re not eligible for this drive yet.</strong>
                    <ul>
                      {modal.eligibility.reasons.map((r) => (
                        <li key={r}>{r}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
              <button
                className="primary full"
                disabled={busy || !modal.eligibility.eligible}
                onClick={() =>
                  mutate(
                    () =>
                      api("/applications", {
                        method: "POST",
                        body: JSON.stringify({ driveId: modal.drive.id }),
                      }),
                    "Application submitted. Your next chapter is underway.",
                  )
                }
              >
                {busy ? "Submitting…" : "Submit application"}
                <Icon name="arrow" size={17} />
              </button>
            </>
          )}
          {modal.type === "drive" && (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget),
                  r = Object.fromEntries(f);
                r.branches = f.getAll("branches");
                for (const k of [
                  "graduationYear",
                  "minCgpa",
                  "maxBacklogs",
                  "packageLpa",
                ])
                  r[k] = Number(r[k]);
                r.deadline = new Date(r.deadline).toISOString();
                r.recruiterUsername = "recruiter";
                mutate(
                  () =>
                    api("/drives", { method: "POST", body: JSON.stringify(r) }),
                  "New drive published.",
                );
              }}
            >
              <div className="form-grid">
                <label>
                  Company
                  <input name="company" required maxLength={120} />
                </label>
                <label>
                  Role title
                  <input name="title" required maxLength={160} />
                </label>
                <label>
                  Location
                  <input name="location" required maxLength={100} />
                </label>
                <label>
                  Annual package (LPA)
                  <input
                    type="number"
                    name="packageLpa"
                    required
                    min="0"
                    max="9999.99"
                    step="0.01"
                  />
                </label>
                <label>
                  Minimum CGPA
                  <input
                    name="minCgpa"
                    type="number"
                    required
                    min="0"
                    max="10"
                    step="0.01"
                    defaultValue="7"
                  />
                </label>
                <label>
                  Maximum active backlogs
                  <input
                    name="maxBacklogs"
                    type="number"
                    required
                    min="0"
                    max="50"
                    defaultValue="0"
                  />
                </label>
                <label>
                  Graduation year
                  <input
                    name="graduationYear"
                    type="number"
                    min="2020"
                    max="2100"
                    required
                    defaultValue="2027"
                  />
                </label>
                <label>
                  Application deadline
                  <input type="datetime-local" name="deadline" required />
                </label>
              </div>
              <fieldset>
                <legend>Eligible branches</legend>
                <div className="checkboxes">
                  {["CSE", "IT", "ECE", "EE", "ME", "CE"].map((b) => (
                    <label key={b}>
                      <input
                        type="checkbox"
                        name="branches"
                        value={b}
                        defaultChecked={["CSE", "IT"].includes(b)}
                      />
                      {b}
                    </label>
                  ))}
                </div>
              </fieldset>
              <label>
                Role description
                <textarea
                  name="description"
                  required
                  maxLength={2000}
                  rows={3}
                />
              </label>
              <p className="muted">Assigned to the demo recruiter account.</p>
              <button className="primary full" disabled={busy}>
                Publish drive
                <Icon name="plus" size={17} />
              </button>
            </form>
          )}
          {modal.type === "academics" && (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget),
                  r = Object.fromEntries(f);
                for (const k of ["graduationYear", "cgpa", "activeBacklogs"])
                  r[k] = Number(r[k]);
                r.verified = f.has("verified");
                mutate(
                  () =>
                    api("/students/" + modal.student.id + "/academics", {
                      method: "PATCH",
                      body: JSON.stringify(r),
                    }),
                  "Academic record updated.",
                );
              }}
            >
              <h3>{modal.student.name}</h3>
              <div className="form-grid">
                <label>
                  Branch
                  <select name="branch" defaultValue={modal.student.branch}>
                    {["CSE", "IT", "ECE", "EE", "ME", "CE"].map((b) => (
                      <option key={b}>{b}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Graduation year
                  <input
                    name="graduationYear"
                    type="number"
                    min="2020"
                    max="2100"
                    required
                    defaultValue={modal.student.graduationYear}
                  />
                </label>
                <label>
                  CGPA
                  <input
                    name="cgpa"
                    type="number"
                    min="0"
                    max="10"
                    step="0.01"
                    required
                    defaultValue={modal.student.cgpa}
                  />
                </label>
                <label>
                  Active backlogs
                  <input
                    name="activeBacklogs"
                    type="number"
                    min="0"
                    max="50"
                    required
                    defaultValue={modal.student.activeBacklogs}
                  />
                </label>
              </div>
              <label className="checkbox">
                <input
                  name="verified"
                  type="checkbox"
                  defaultChecked={modal.student.verified}
                />
                Academic details verified
              </label>
              <button className="primary full" disabled={busy}>
                Save academic record
              </button>
            </form>
          )}
          {modal.type === "application" && (
            <>
              <div className="application-heading">
                <div>
                  <h2>{modal.application.company}</h2>
                  <p>{modal.application.driveTitle}</p>
                </div>
                <Badge status={modal.application.status} />
              </div>
              <dl className="detail-list">
                <dt>Student</dt>
                <dd>{modal.application.studentName}</dd>
                <dt>Academic snapshot</dt>
                <dd>
                  {modal.application.branch} ·{" "}
                  {Number(modal.application.cgpa).toFixed(2)} CGPA
                </dd>
                {modal.application.interviewAt && (
                  <>
                    <dt>Interview</dt>
                    <dd>{dateTime(modal.application.interviewAt)}</dd>
                    <dt>Meeting link</dt>
                    <dd>
                      <a
                        href={modal.application.meetingUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Open interview meeting ↗
                      </a>
                    </dd>
                  </>
                )}
              </dl>
              <h3>Application timeline</h3>
              <div className="timeline">
                {modal.history.map((h) => (
                  <div key={h.id}>
                    <span />
                    <strong>{label(h.status)}</strong>
                    <small>
                      {dateTime(h.changedAt)} · {h.changedBy}
                    </small>
                    {h.note && <p>{h.note}</p>}
                  </div>
                ))}
              </div>
              {!["SELECTED", "REJECTED", "WITHDRAWN"].includes(
                modal.application.status,
              ) && (
                <div className="modal-actions">
                  {isStudent ? (
                    <button
                      className="secondary danger"
                      onClick={() =>
                        setModal({
                          type: "status",
                          application: modal.application,
                          next: "WITHDRAWN",
                        })
                      }
                    >
                      Withdraw application
                    </button>
                  ) : (
                    <>
                      {{
                        SUBMITTED: ["SHORTLISTED", "REJECTED"],
                        SHORTLISTED: ["INTERVIEW_SCHEDULED", "REJECTED"],
                        INTERVIEW_SCHEDULED: ["SELECTED", "REJECTED"],
                      }[modal.application.status]?.map((s) => (
                        <button
                          className={
                            s === "REJECTED" ? "secondary danger" : "primary"
                          }
                          key={s}
                          onClick={() =>
                            setModal({
                              type: "status",
                              application: modal.application,
                              next: s,
                            })
                          }
                        >
                          {label(s)}
                        </button>
                      ))}
                    </>
                  )}
                </div>
              )}
            </>
          )}
          {modal.type === "status" && (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget),
                  r = { status: modal.next, note: f.get("note") };
                if (modal.next === "INTERVIEW_SCHEDULED") {
                  r.interviewAt = new Date(f.get("interviewAt")).toISOString();
                  r.meetingUrl = f.get("meetingUrl");
                }
                mutate(
                  () =>
                    api("/applications/" + modal.application.id + "/status", {
                      method: "PATCH",
                      body: JSON.stringify(r),
                    }),
                  "Application updated.",
                );
              }}
            >
              <p>
                Move <strong>{modal.application.studentName}</strong>’s
                application at <strong>{modal.application.company}</strong> to{" "}
                <Badge status={modal.next} />.
              </p>
              {modal.next === "WITHDRAWN" && (
                <p className="muted">
                  You won’t be able to apply to this drive again in this demo.
                </p>
              )}
              {modal.next === "INTERVIEW_SCHEDULED" && (
                <>
                  <label>
                    Interview time
                    <input type="datetime-local" name="interviewAt" required />
                  </label>
                  <label>
                    Meeting URL
                    <input
                      type="url"
                      name="meetingUrl"
                      placeholder="https://…"
                      required
                      maxLength={500}
                    />
                  </label>
                </>
              )}
              <label>
                Note
                <textarea name="note" rows={3} maxLength={500} />
              </label>
              <button className="primary full" disabled={busy}>
                Confirm update
                <Icon name="check" size={17} />
              </button>
            </form>
          )}
        </Modal>
      )}
    </div>
  );
}
function Empty({ icon, title, text }) {
  return (
    <div className="empty">
      <span className="empty-icon">
        <Icon name={icon} size={25} />
      </span>
      <h3>{title}</h3>
      <p>{text}</p>
    </div>
  );
}

createRoot(document.getElementById("root")).render(<App />);
