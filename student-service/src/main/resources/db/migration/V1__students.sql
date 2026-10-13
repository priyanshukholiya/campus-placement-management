CREATE TABLE students (
    id VARCHAR(80) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    branch VARCHAR(20) NOT NULL,
    graduation_year INTEGER NOT NULL,
    cgpa NUMERIC(4,2) NOT NULL CHECK (cgpa >= 0 AND cgpa <= 10),
    active_backlogs INTEGER NOT NULL CHECK (active_backlogs >= 0),
    verified BOOLEAN NOT NULL,
    skills VARCHAR(1000) NOT NULL DEFAULT '',
    version BIGINT NOT NULL DEFAULT 0
);
