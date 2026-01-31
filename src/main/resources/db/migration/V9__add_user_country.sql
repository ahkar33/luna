-- Add country fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE users ADD COLUMN IF NOT EXISTS country VARCHAR(100);

-- Index for country-based queries
CREATE INDEX IF NOT EXISTS idx_users_country_code ON users(country_code);
