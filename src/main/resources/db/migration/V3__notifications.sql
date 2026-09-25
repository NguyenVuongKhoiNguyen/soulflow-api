CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    event VARCHAR(50) NOT NULL,
    message NVARCHAR(500) NOT NULL,
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    is_read BIT NOT NULL DEFAULT 0
);

CREATE INDEX ix_notifications_created_date ON notifications(created_date DESC);
CREATE INDEX ix_notifications_is_read ON notifications(is_read);
