CREATE TABLE sepay_webhook_receipts (
    transaction_id BIGINT PRIMARY KEY,
    payload NVARCHAR(MAX) NOT NULL,
    received_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    processed BIT NOT NULL DEFAULT 0
);
