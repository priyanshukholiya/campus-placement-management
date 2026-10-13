CREATE TABLE placement_applications (
 id VARCHAR(36) PRIMARY KEY, student_id VARCHAR(80) NOT NULL, drive_id VARCHAR(36) NOT NULL,
 student_name VARCHAR(120) NOT NULL, branch VARCHAR(20) NOT NULL, cgpa NUMERIC(4,2) NOT NULL,
 graduation_year INTEGER NOT NULL, active_backlogs INTEGER NOT NULL,
 company VARCHAR(120) NOT NULL, drive_title VARCHAR(160) NOT NULL, recruiter_username VARCHAR(80) NOT NULL,
 eligibility_snapshot VARCHAR(1000) NOT NULL, status VARCHAR(30) NOT NULL,
 interview_at TIMESTAMP WITH TIME ZONE, meeting_url VARCHAR(500),
 applied_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT uq_student_drive UNIQUE(student_id,drive_id)
);
CREATE INDEX ix_app_recruiter ON placement_applications(recruiter_username);
CREATE TABLE status_history (
 id VARCHAR(36) PRIMARY KEY, application_id VARCHAR(36) NOT NULL REFERENCES placement_applications(id),
 status VARCHAR(30) NOT NULL, changed_by VARCHAR(80) NOT NULL, note VARCHAR(500) NOT NULL,
 changed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_history_application ON status_history(application_id);
CREATE TABLE outbox_events (
 id VARCHAR(36) PRIMARY KEY, recipient VARCHAR(80) NOT NULL, title VARCHAR(160) NOT NULL,
 message VARCHAR(1000) NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
 delivery_status VARCHAR(20) NOT NULL, attempts INTEGER NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMP WITH TIME ZONE, last_error VARCHAR(500)
);
CREATE INDEX ix_outbox_pending ON outbox_events(delivery_status,occurred_at);
