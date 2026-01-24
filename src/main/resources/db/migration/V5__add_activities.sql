-- Create activities table
CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    target_user_id BIGINT,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_user_id ON activities(user_id);
CREATE INDEX idx_activity_type ON activities(activity_type);
CREATE INDEX idx_activity_created_at ON activities(created_at DESC);
CREATE INDEX idx_activity_target_user_id ON activities(target_user_id);
CREATE INDEX idx_activity_entity ON activities(entity_type, entity_id);
