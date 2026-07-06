CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(30) UNIQUE NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(1024),
    level INTEGER NOT NULL DEFAULT 1,
    xp BIGINT NOT NULL DEFAULT 0,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE login_otps (
    id UUID PRIMARY KEY,
    phone VARCHAR(30) NOT NULL,
    otp VARCHAR(8) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id UUID REFERENCES users (id),
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE
);

CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL REFERENCES users (id),
    balance BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    type VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    source VARCHAR(100) NOT NULL,
    reference_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE matches (
    id UUID PRIMARY KEY,
    game_type VARCHAR(50) NOT NULL,
    mode VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    room_code VARCHAR(32) UNIQUE,
    min_players INTEGER NOT NULL,
    max_players INTEGER NOT NULL,
    stake BIGINT NOT NULL,
    server_state_json TEXT NOT NULL,
    winner_user_id UUID,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE TABLE match_players (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES matches (id),
    user_id UUID NOT NULL REFERENCES users (id),
    seat_no INTEGER NOT NULL,
    chips_committed BIGINT NOT NULL,
    result VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP NOT NULL
);

CREATE TABLE friends (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    friend_user_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, friend_user_id)
);

CREATE TABLE daily_rewards (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    reward_date DATE NOT NULL,
    chips BIGINT NOT NULL,
    UNIQUE (user_id, reward_date)
);

CREATE TABLE missions (
    id UUID PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    reward_chips BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE shop_items (
    id UUID PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    chips BIGINT NOT NULL,
    price_label VARCHAR(50) NOT NULL,
    price_amount_paise BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    vip BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE purchases (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    shop_item_id UUID NOT NULL REFERENCES shop_items (id),
    platform VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount_paise BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    provider_order_id VARCHAR(255) UNIQUE,
    provider_payment_id VARCHAR(255) UNIQUE,
    receipt_reference VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP
);

CREATE TABLE advertisement_rewards (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    placement VARCHAR(100) NOT NULL,
    chips BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    title VARCHAR(255) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    route VARCHAR(255),
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

INSERT INTO missions (id, code, title, reward_chips, active) VALUES
('11111111-1111-1111-1111-111111111111', 'PLAY_3_MATCHES', 'Play 3 matches', 750, TRUE),
('22222222-2222-2222-2222-222222222222', 'WIN_1_MATCH', 'Win 1 match', 1000, TRUE),
('33333333-3333-3333-3333-333333333333', 'WATCH_REWARDED_AD', 'Watch a rewarded ad', 500, TRUE);

INSERT INTO shop_items (id, code, name, chips, price_label, price_amount_paise, currency, vip, active) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'STARTER_PACK', 'Starter Pack', 10000, 'INR 99', 9900, 'INR', FALSE, TRUE),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'SILVER_PACK', 'Silver Pack', 50000, 'INR 399', 39900, 'INR', FALSE, TRUE),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'GOLD_PACK', 'Gold Pack', 150000, 'INR 999', 99900, 'INR', FALSE, TRUE),
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'DIAMOND_PACK', 'Diamond Pack', 500000, 'INR 2499', 249900, 'INR', FALSE, TRUE),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'VIP_MONTHLY', 'VIP Membership', 50000, 'INR 499/mo', 49900, 'INR', TRUE, TRUE);
