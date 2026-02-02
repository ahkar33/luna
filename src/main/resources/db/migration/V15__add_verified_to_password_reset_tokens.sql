-- Add verified column to password_reset_tokens
ALTER TABLE password_reset_tokens
ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for verified column
CREATE INDEX idx_password_reset_tokens_verified ON password_reset_tokens(user_id, verified);
