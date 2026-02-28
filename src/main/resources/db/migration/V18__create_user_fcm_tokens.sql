CREATE TABLE user_fcm_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token    TEXT          NOT NULL,
    platform     VARCHAR(20),
    device_name  VARCHAR(100),
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fcm_token UNIQUE (fcm_token)
);

CREATE INDEX idx_user_fcm_tokens_user_id ON user_fcm_tokens(user_id);
