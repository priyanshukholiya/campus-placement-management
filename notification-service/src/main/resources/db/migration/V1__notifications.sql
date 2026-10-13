CREATE TABLE notifications (
 id VARCHAR(36) PRIMARY KEY, recipient VARCHAR(80) NOT NULL, title VARCHAR(160) NOT NULL,
 message VARCHAR(1000) NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
 read_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_notifications_recipient ON notifications(recipient,occurred_at);
