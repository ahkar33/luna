-- Add media columns to posts table
ALTER TABLE posts
ADD COLUMN image_urls TEXT,
ADD COLUMN video_urls TEXT;

-- Add profile image column to users table
ALTER TABLE users
ADD COLUMN profile_image_url VARCHAR(500);

-- Add indexes for better query performance
CREATE INDEX idx_posts_image_urls ON posts(image_urls) WHERE image_urls IS NOT NULL;
CREATE INDEX idx_posts_video_urls ON posts(video_urls) WHERE video_urls IS NOT NULL;
CREATE INDEX idx_users_profile_image_url ON users(profile_image_url) WHERE profile_image_url IS NOT NULL;
