-- Add OAuth support fields to users table
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);

-- Make password nullable (Google OAuth users won't have a password)
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Index for looking up users by OAuth provider
CREATE INDEX idx_users_provider ON users(auth_provider, provider_id);
