CREATE TABLE drives (
    id VARCHAR(36) PRIMARY KEY, company VARCHAR(120) NOT NULL,
    title VARCHAR(160) NOT NULL, location VARCHAR(100) NOT NULL,
    description VARCHAR(2000) NOT NULL, recruiter_username VARCHAR(80) NOT NULL,
    branches VARCHAR(100) NOT NULL, graduation_year INTEGER NOT NULL,
    min_cgpa NUMERIC(4,2) NOT NULL CHECK (min_cgpa >= 0 AND min_cgpa <= 10),
    max_backlogs INTEGER NOT NULL CHECK (max_backlogs >= 0),
    package_lpa NUMERIC(6,2) NOT NULL, deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
