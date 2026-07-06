CREATE TABLE platform_accounts (
    id UUID PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    balance BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE platform_account_transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES platform_accounts (id),
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    source VARCHAR(100) NOT NULL,
    reference_id VARCHAR(255),
    user_id UUID,
    created_at TIMESTAMP NOT NULL
);

INSERT INTO platform_accounts (id, code, balance, updated_at) VALUES
('99999999-9999-9999-9999-999999999999', 'HOUSE', 0, NOW());
