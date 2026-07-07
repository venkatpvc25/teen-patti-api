CREATE TABLE withdrawals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    status VARCHAR(50) NOT NULL,
    chips BIGINT NOT NULL,
    amount_paise BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    payout_mode VARCHAR(50) NOT NULL,
    upi_id VARCHAR(255),
    razorpay_contact_id VARCHAR(255),
    razorpay_fund_account_id VARCHAR(255),
    razorpay_payout_id VARCHAR(255) UNIQUE,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE INDEX idx_withdrawals_user_created_at ON withdrawals (user_id, created_at DESC);
CREATE INDEX idx_withdrawals_status ON withdrawals (status);
