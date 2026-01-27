-- Drop old columns if they exist
ALTER TABLE posts
DROP COLUMN IF EXISTS image_url_1,
DROP COLUMN IF EXISTS image_url_2,
DROP COLUMN IF EXISTS image_url_3,
DROP COLUMN IF EXISTS video_url;

-- Add new JSON array columns
ALTER TABLE posts
ADD COLUMN IF NOT EXISTS image_urls TEXT,
ADD COLUMN IF NOT EXISTS video_urls TEXT;

-- Add profile image column to users table if not exists
ALTER TABLE users
ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500);

-- Drop old indexes if they exist
DROP INDEX IF EXISTS idx_posts_image_url_1;
DROP INDEX IF EXISTS idx_posts_video_url;

-- Add new indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_posts_image_urls ON posts(image_urls) WHERE image_urls IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_posts_video_urls ON posts(video_urls) WHERE video_urls IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_profile_image_url ON users(profile_image_url) WHERE profile_image_url IS NOT NULL;
