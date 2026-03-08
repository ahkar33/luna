-- V19: Migrate all primary keys and foreign keys from BIGSERIAL/BIGINT to UUID
-- Strategy: add new UUID columns, backfill via JOIN, drop old columns, rename, re-add constraints

-- ============================================================
-- STEP 1: Drop all FK constraints (child → parent order)
-- ============================================================

-- post_hashtags
ALTER TABLE post_hashtags DROP CONSTRAINT IF EXISTS post_hashtags_post_id_fkey;
ALTER TABLE post_hashtags DROP CONSTRAINT IF EXISTS post_hashtags_hashtag_id_fkey;

-- post_likes
ALTER TABLE post_likes DROP CONSTRAINT IF EXISTS fk_like_post;
ALTER TABLE post_likes DROP CONSTRAINT IF EXISTS fk_like_user;

-- saved_posts
ALTER TABLE saved_posts DROP CONSTRAINT IF EXISTS saved_posts_user_id_fkey;
ALTER TABLE saved_posts DROP CONSTRAINT IF EXISTS saved_posts_post_id_fkey;

-- reposts
ALTER TABLE reposts DROP CONSTRAINT IF EXISTS reposts_user_id_fkey;
ALTER TABLE reposts DROP CONSTRAINT IF EXISTS reposts_post_id_fkey;

-- comments (self-referential + FK to posts + users)
ALTER TABLE comments DROP CONSTRAINT IF EXISTS comments_post_id_fkey;
ALTER TABLE comments DROP CONSTRAINT IF EXISTS comments_user_id_fkey;
ALTER TABLE comments DROP CONSTRAINT IF EXISTS comments_parent_id_fkey;

-- user-owned tables
ALTER TABLE refresh_tokens      DROP CONSTRAINT IF EXISTS fk_refresh_token_user;
ALTER TABLE verification_tokens DROP CONSTRAINT IF EXISTS fk_verification_user;
ALTER TABLE user_devices        DROP CONSTRAINT IF EXISTS fk_device_user;
ALTER TABLE device_verification_tokens DROP CONSTRAINT IF EXISTS fk_device_token_user;
ALTER TABLE password_reset_tokens      DROP CONSTRAINT IF EXISTS password_reset_tokens_user_id_fkey;
ALTER TABLE user_fcm_tokens     DROP CONSTRAINT IF EXISTS user_fcm_tokens_user_id_fkey;
ALTER TABLE activities          DROP CONSTRAINT IF EXISTS fk_activity_user;
ALTER TABLE posts               DROP CONSTRAINT IF EXISTS fk_post_user;

-- user_follows (two FKs to users)
ALTER TABLE user_follows DROP CONSTRAINT IF EXISTS fk_follower;
ALTER TABLE user_follows DROP CONSTRAINT IF EXISTS fk_following;

-- ============================================================
-- STEP 2: Drop UNIQUE constraints that reference old FK columns
-- (CHECK constraints on dropped columns are auto-removed by PG)
-- ============================================================

ALTER TABLE user_follows  DROP CONSTRAINT IF EXISTS unique_follow;
ALTER TABLE user_follows  DROP CONSTRAINT IF EXISTS check_not_self_follow;
ALTER TABLE post_likes    DROP CONSTRAINT IF EXISTS unique_post_like;
ALTER TABLE saved_posts   DROP CONSTRAINT IF EXISTS unique_user_saved_post;
ALTER TABLE reposts       DROP CONSTRAINT IF EXISTS unique_user_repost;
ALTER TABLE post_hashtags DROP CONSTRAINT IF EXISTS unique_post_hashtag;
ALTER TABLE user_devices  DROP CONSTRAINT IF EXISTS uk_user_device;

-- ============================================================
-- STEP 3: Add new UUID PK columns to ALL tables
-- ============================================================

ALTER TABLE users                      ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE hashtags                   ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE refresh_tokens             ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE verification_tokens        ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE user_devices               ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE device_verification_tokens ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE password_reset_tokens      ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE user_fcm_tokens            ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE user_follows               ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE posts                      ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE activities                 ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE post_likes                 ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE saved_posts                ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE reposts                    ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE post_hashtags              ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE comments                   ADD COLUMN IF NOT EXISTS new_id UUID NOT NULL DEFAULT gen_random_uuid();

-- ============================================================
-- STEP 4: Add new UUID FK columns to child tables (nullable for backfill)
-- ============================================================

-- user_id FK columns
ALTER TABLE refresh_tokens             ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE verification_tokens        ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE user_devices               ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE device_verification_tokens ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE password_reset_tokens      ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE user_fcm_tokens            ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE posts                      ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE activities                 ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE post_likes                 ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE saved_posts                ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE reposts                    ADD COLUMN IF NOT EXISTS new_user_id UUID;
ALTER TABLE comments                   ADD COLUMN IF NOT EXISTS new_user_id UUID;

-- user_follows: two user FK columns
ALTER TABLE user_follows ADD COLUMN IF NOT EXISTS new_follower_id  UUID;
ALTER TABLE user_follows ADD COLUMN IF NOT EXISTS new_following_id UUID;

-- post_id FK columns
ALTER TABLE post_likes    ADD COLUMN IF NOT EXISTS new_post_id UUID;
ALTER TABLE saved_posts   ADD COLUMN IF NOT EXISTS new_post_id UUID;
ALTER TABLE reposts       ADD COLUMN IF NOT EXISTS new_post_id UUID;
ALTER TABLE post_hashtags ADD COLUMN IF NOT EXISTS new_post_id UUID;
ALTER TABLE comments      ADD COLUMN IF NOT EXISTS new_post_id UUID;

-- hashtag_id FK column
ALTER TABLE post_hashtags ADD COLUMN IF NOT EXISTS new_hashtag_id UUID;

-- comments self-reference
ALTER TABLE comments ADD COLUMN IF NOT EXISTS new_parent_id UUID;

-- activities: target_user_id (has no FK constraint, but references users)
-- entity_id: was a generic BIGINT reference (no FK), will be nullable UUID after migration
ALTER TABLE activities ADD COLUMN IF NOT EXISTS new_target_user_id UUID;
ALTER TABLE activities ADD COLUMN IF NOT EXISTS new_entity_id      UUID;

-- ============================================================
-- STEP 5: Backfill all UUID FK columns via JOIN on old integer ids
-- ============================================================

-- user_id → users.new_id
UPDATE refresh_tokens t             SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE verification_tokens t        SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE user_devices t               SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE device_verification_tokens t SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE password_reset_tokens t      SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE user_fcm_tokens t            SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE posts t                      SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE activities t                 SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE post_likes t                 SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE saved_posts t                SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE reposts t                    SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;
UPDATE comments t                   SET new_user_id = u.new_id FROM users u WHERE u.id = t.user_id;

-- user_follows: follower_id and following_id
UPDATE user_follows t SET new_follower_id  = u.new_id FROM users u WHERE u.id = t.follower_id;
UPDATE user_follows t SET new_following_id = u.new_id FROM users u WHERE u.id = t.following_id;

-- post_id → posts.new_id
UPDATE post_likes t    SET new_post_id = p.new_id FROM posts p WHERE p.id = t.post_id;
UPDATE saved_posts t   SET new_post_id = p.new_id FROM posts p WHERE p.id = t.post_id;
UPDATE reposts t       SET new_post_id = p.new_id FROM posts p WHERE p.id = t.post_id;
UPDATE post_hashtags t SET new_post_id = p.new_id FROM posts p WHERE p.id = t.post_id;
UPDATE comments t      SET new_post_id = p.new_id FROM posts p WHERE p.id = t.post_id;

-- hashtag_id → hashtags.new_id
UPDATE post_hashtags t SET new_hashtag_id = h.new_id FROM hashtags h WHERE h.id = t.hashtag_id;

-- comments self-reference: parent_id → comments.new_id
UPDATE comments c SET new_parent_id = p.new_id FROM comments p WHERE p.id = c.parent_id;

-- activities.target_user_id (references users, backfill where possible)
UPDATE activities t SET new_target_user_id = u.new_id FROM users u WHERE u.id = t.target_user_id;

-- activities.entity_id: generic integer reference with no FK — set to NULL.
-- Old integer IDs are meaningless after UUID migration. New activities will populate this with a UUID.
-- new_entity_id stays NULL for all existing rows (no UPDATE needed).

-- ============================================================
-- STEP 6: Drop old PK constraints (must happen before dropping id columns)
-- ============================================================

ALTER TABLE users                      DROP CONSTRAINT users_pkey CASCADE;
ALTER TABLE hashtags                   DROP CONSTRAINT hashtags_pkey CASCADE;
ALTER TABLE refresh_tokens             DROP CONSTRAINT refresh_tokens_pkey CASCADE;
ALTER TABLE verification_tokens        DROP CONSTRAINT verification_tokens_pkey CASCADE;
ALTER TABLE user_devices               DROP CONSTRAINT user_devices_pkey CASCADE;
ALTER TABLE device_verification_tokens DROP CONSTRAINT device_verification_tokens_pkey CASCADE;
ALTER TABLE password_reset_tokens      DROP CONSTRAINT password_reset_tokens_pkey CASCADE;
ALTER TABLE user_fcm_tokens            DROP CONSTRAINT user_fcm_tokens_pkey CASCADE;
ALTER TABLE user_follows               DROP CONSTRAINT user_follows_pkey CASCADE;
ALTER TABLE posts                      DROP CONSTRAINT posts_pkey CASCADE;
ALTER TABLE activities                 DROP CONSTRAINT activities_pkey CASCADE;
ALTER TABLE post_likes                 DROP CONSTRAINT post_likes_pkey CASCADE;
ALTER TABLE saved_posts                DROP CONSTRAINT saved_posts_pkey CASCADE;
ALTER TABLE reposts                    DROP CONSTRAINT reposts_pkey CASCADE;
ALTER TABLE post_hashtags              DROP CONSTRAINT post_hashtags_pkey CASCADE;
ALTER TABLE comments                   DROP CONSTRAINT comments_pkey CASCADE;

-- ============================================================
-- STEP 7: Drop old integer id and FK columns
-- (PG auto-drops any remaining indexes referencing dropped columns)
-- ============================================================

-- PK id columns
ALTER TABLE users                      DROP COLUMN id;
ALTER TABLE hashtags                   DROP COLUMN id;
ALTER TABLE refresh_tokens             DROP COLUMN id;
ALTER TABLE verification_tokens        DROP COLUMN id;
ALTER TABLE user_devices               DROP COLUMN id;
ALTER TABLE device_verification_tokens DROP COLUMN id;
ALTER TABLE password_reset_tokens      DROP COLUMN id;
ALTER TABLE user_fcm_tokens            DROP COLUMN id;
ALTER TABLE user_follows               DROP COLUMN id;
ALTER TABLE posts                      DROP COLUMN id;
ALTER TABLE activities                 DROP COLUMN id;
ALTER TABLE post_likes                 DROP COLUMN id;
ALTER TABLE saved_posts                DROP COLUMN id;
ALTER TABLE reposts                    DROP COLUMN id;
ALTER TABLE post_hashtags              DROP COLUMN id;
ALTER TABLE comments                   DROP COLUMN id;

-- Old user_id FK columns
ALTER TABLE refresh_tokens             DROP COLUMN user_id;
ALTER TABLE verification_tokens        DROP COLUMN user_id;
ALTER TABLE user_devices               DROP COLUMN user_id;
ALTER TABLE device_verification_tokens DROP COLUMN user_id;
ALTER TABLE password_reset_tokens      DROP COLUMN user_id;
ALTER TABLE user_fcm_tokens            DROP COLUMN user_id;
ALTER TABLE posts                      DROP COLUMN user_id;
ALTER TABLE activities                 DROP COLUMN user_id;
ALTER TABLE post_likes                 DROP COLUMN user_id;
ALTER TABLE saved_posts                DROP COLUMN user_id;
ALTER TABLE reposts                    DROP COLUMN user_id;
ALTER TABLE comments                   DROP COLUMN user_id;

-- user_follows old FK columns
ALTER TABLE user_follows DROP COLUMN follower_id;
ALTER TABLE user_follows DROP COLUMN following_id;

-- Old post_id FK columns
ALTER TABLE post_likes    DROP COLUMN post_id;
ALTER TABLE saved_posts   DROP COLUMN post_id;
ALTER TABLE reposts       DROP COLUMN post_id;
ALTER TABLE post_hashtags DROP COLUMN post_id;
ALTER TABLE comments      DROP COLUMN post_id;

-- Other old FK columns
ALTER TABLE post_hashtags DROP COLUMN hashtag_id;
ALTER TABLE comments      DROP COLUMN parent_id;

-- Old activities generic-reference columns
ALTER TABLE activities DROP COLUMN target_user_id;
ALTER TABLE activities DROP COLUMN entity_id;

-- ============================================================
-- STEP 8: Rename new_* columns to their final names
-- ============================================================

-- PK id columns
ALTER TABLE users                      RENAME COLUMN new_id TO id;
ALTER TABLE hashtags                   RENAME COLUMN new_id TO id;
ALTER TABLE refresh_tokens             RENAME COLUMN new_id TO id;
ALTER TABLE verification_tokens        RENAME COLUMN new_id TO id;
ALTER TABLE user_devices               RENAME COLUMN new_id TO id;
ALTER TABLE device_verification_tokens RENAME COLUMN new_id TO id;
ALTER TABLE password_reset_tokens      RENAME COLUMN new_id TO id;
ALTER TABLE user_fcm_tokens            RENAME COLUMN new_id TO id;
ALTER TABLE user_follows               RENAME COLUMN new_id TO id;
ALTER TABLE posts                      RENAME COLUMN new_id TO id;
ALTER TABLE activities                 RENAME COLUMN new_id TO id;
ALTER TABLE post_likes                 RENAME COLUMN new_id TO id;
ALTER TABLE saved_posts                RENAME COLUMN new_id TO id;
ALTER TABLE reposts                    RENAME COLUMN new_id TO id;
ALTER TABLE post_hashtags              RENAME COLUMN new_id TO id;
ALTER TABLE comments                   RENAME COLUMN new_id TO id;

-- user_id FK columns
ALTER TABLE refresh_tokens             RENAME COLUMN new_user_id TO user_id;
ALTER TABLE verification_tokens        RENAME COLUMN new_user_id TO user_id;
ALTER TABLE user_devices               RENAME COLUMN new_user_id TO user_id;
ALTER TABLE device_verification_tokens RENAME COLUMN new_user_id TO user_id;
ALTER TABLE password_reset_tokens      RENAME COLUMN new_user_id TO user_id;
ALTER TABLE user_fcm_tokens            RENAME COLUMN new_user_id TO user_id;
ALTER TABLE posts                      RENAME COLUMN new_user_id TO user_id;
ALTER TABLE activities                 RENAME COLUMN new_user_id TO user_id;
ALTER TABLE post_likes                 RENAME COLUMN new_user_id TO user_id;
ALTER TABLE saved_posts                RENAME COLUMN new_user_id TO user_id;
ALTER TABLE reposts                    RENAME COLUMN new_user_id TO user_id;
ALTER TABLE comments                   RENAME COLUMN new_user_id TO user_id;

-- user_follows
ALTER TABLE user_follows RENAME COLUMN new_follower_id  TO follower_id;
ALTER TABLE user_follows RENAME COLUMN new_following_id TO following_id;

-- post_id FK columns
ALTER TABLE post_likes    RENAME COLUMN new_post_id TO post_id;
ALTER TABLE saved_posts   RENAME COLUMN new_post_id TO post_id;
ALTER TABLE reposts       RENAME COLUMN new_post_id TO post_id;
ALTER TABLE post_hashtags RENAME COLUMN new_post_id TO post_id;
ALTER TABLE comments      RENAME COLUMN new_post_id TO post_id;

-- Other FK columns
ALTER TABLE post_hashtags RENAME COLUMN new_hashtag_id    TO hashtag_id;
ALTER TABLE comments      RENAME COLUMN new_parent_id     TO parent_id;
ALTER TABLE activities    RENAME COLUMN new_target_user_id TO target_user_id;
ALTER TABLE activities    RENAME COLUMN new_entity_id      TO entity_id;

-- ============================================================
-- STEP 9: Add NOT NULL constraints on FK columns (were nullable during migration)
-- ============================================================

ALTER TABLE refresh_tokens             ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE verification_tokens        ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE user_devices               ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE device_verification_tokens ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE password_reset_tokens      ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE user_fcm_tokens            ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE posts                      ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE activities                 ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE post_likes                 ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE post_likes                 ALTER COLUMN post_id SET NOT NULL;
ALTER TABLE saved_posts                ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE saved_posts                ALTER COLUMN post_id SET NOT NULL;
ALTER TABLE reposts                    ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE reposts                    ALTER COLUMN post_id SET NOT NULL;
ALTER TABLE post_hashtags              ALTER COLUMN post_id SET NOT NULL;
ALTER TABLE post_hashtags              ALTER COLUMN hashtag_id SET NOT NULL;
ALTER TABLE comments                   ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE comments                   ALTER COLUMN post_id SET NOT NULL;
ALTER TABLE user_follows               ALTER COLUMN follower_id  SET NOT NULL;
ALTER TABLE user_follows               ALTER COLUMN following_id SET NOT NULL;
-- activities.target_user_id and entity_id remain nullable

-- ============================================================
-- STEP 10: Add PK constraints on all tables
-- ============================================================

ALTER TABLE users                      ADD PRIMARY KEY (id);
ALTER TABLE hashtags                   ADD PRIMARY KEY (id);
ALTER TABLE refresh_tokens             ADD PRIMARY KEY (id);
ALTER TABLE verification_tokens        ADD PRIMARY KEY (id);
ALTER TABLE user_devices               ADD PRIMARY KEY (id);
ALTER TABLE device_verification_tokens ADD PRIMARY KEY (id);
ALTER TABLE password_reset_tokens      ADD PRIMARY KEY (id);
ALTER TABLE user_fcm_tokens            ADD PRIMARY KEY (id);
ALTER TABLE user_follows               ADD PRIMARY KEY (id);
ALTER TABLE posts                      ADD PRIMARY KEY (id);
ALTER TABLE activities                 ADD PRIMARY KEY (id);
ALTER TABLE post_likes                 ADD PRIMARY KEY (id);
ALTER TABLE saved_posts                ADD PRIMARY KEY (id);
ALTER TABLE reposts                    ADD PRIMARY KEY (id);
ALTER TABLE post_hashtags              ADD PRIMARY KEY (id);
ALTER TABLE comments                   ADD PRIMARY KEY (id);

-- ============================================================
-- STEP 11: Re-add FK constraints
-- ============================================================

ALTER TABLE refresh_tokens             ADD CONSTRAINT fk_refresh_token_user   FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE verification_tokens        ADD CONSTRAINT fk_verification_user     FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_devices               ADD CONSTRAINT fk_device_user           FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE device_verification_tokens ADD CONSTRAINT fk_device_token_user     FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE password_reset_tokens      ADD CONSTRAINT fk_password_reset_user   FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_fcm_tokens            ADD CONSTRAINT fk_fcm_token_user        FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_follows               ADD CONSTRAINT fk_follower              FOREIGN KEY (follower_id)  REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_follows               ADD CONSTRAINT fk_following             FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE posts                      ADD CONSTRAINT fk_post_user             FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE activities                 ADD CONSTRAINT fk_activity_user         FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE post_likes                 ADD CONSTRAINT fk_like_post             FOREIGN KEY (post_id)      REFERENCES posts(id)    ON DELETE CASCADE;
ALTER TABLE post_likes                 ADD CONSTRAINT fk_like_user             FOREIGN KEY (user_id)      REFERENCES users(id)    ON DELETE CASCADE;
ALTER TABLE saved_posts                ADD CONSTRAINT fk_saved_post            FOREIGN KEY (post_id)      REFERENCES posts(id)    ON DELETE CASCADE;
ALTER TABLE saved_posts                ADD CONSTRAINT fk_saved_user            FOREIGN KEY (user_id)      REFERENCES users(id)    ON DELETE CASCADE;
ALTER TABLE reposts                    ADD CONSTRAINT fk_repost_post           FOREIGN KEY (post_id)      REFERENCES posts(id)    ON DELETE CASCADE;
ALTER TABLE reposts                    ADD CONSTRAINT fk_repost_user           FOREIGN KEY (user_id)      REFERENCES users(id)    ON DELETE CASCADE;
ALTER TABLE post_hashtags              ADD CONSTRAINT fk_ph_post               FOREIGN KEY (post_id)      REFERENCES posts(id)    ON DELETE CASCADE;
ALTER TABLE post_hashtags              ADD CONSTRAINT fk_ph_hashtag            FOREIGN KEY (hashtag_id)   REFERENCES hashtags(id) ON DELETE CASCADE;
ALTER TABLE comments                   ADD CONSTRAINT fk_comment_post          FOREIGN KEY (post_id)      REFERENCES posts(id)    ON DELETE CASCADE;
ALTER TABLE comments                   ADD CONSTRAINT fk_comment_user          FOREIGN KEY (user_id)      REFERENCES users(id)    ON DELETE CASCADE;
ALTER TABLE comments                   ADD CONSTRAINT fk_comment_parent        FOREIGN KEY (parent_id)    REFERENCES comments(id) ON DELETE CASCADE;

-- ============================================================
-- STEP 12: Re-add UNIQUE and CHECK constraints
-- ============================================================

ALTER TABLE user_follows  ADD CONSTRAINT unique_follow         UNIQUE (follower_id, following_id);
ALTER TABLE user_follows  ADD CONSTRAINT check_not_self_follow CHECK  (follower_id != following_id);
ALTER TABLE post_likes    ADD CONSTRAINT unique_post_like      UNIQUE (post_id, user_id);
ALTER TABLE saved_posts   ADD CONSTRAINT unique_user_saved_post UNIQUE (user_id, post_id);
ALTER TABLE reposts       ADD CONSTRAINT unique_user_repost    UNIQUE (user_id, post_id);
ALTER TABLE post_hashtags ADD CONSTRAINT unique_post_hashtag   UNIQUE (post_id, hashtag_id);
ALTER TABLE user_devices  ADD CONSTRAINT uk_user_device        UNIQUE (user_id, device_fingerprint);

-- ============================================================
-- STEP 13: Recreate indexes on new UUID FK columns
-- (Indexes on dropped columns were auto-removed by PostgreSQL)
-- ============================================================

CREATE INDEX idx_refresh_tokens_user_id         ON refresh_tokens(user_id);
CREATE INDEX idx_verification_otp_user          ON verification_tokens(otp, user_id);
CREATE INDEX idx_user_devices_user              ON user_devices(user_id);
CREATE INDEX idx_device_tokens_otp              ON device_verification_tokens(otp, user_id, device_fingerprint);
CREATE INDEX idx_password_reset_tokens_user_id  ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_otp_user ON password_reset_tokens(otp, user_id);
CREATE INDEX idx_user_fcm_tokens_user_id        ON user_fcm_tokens(user_id);
CREATE INDEX idx_user_follows_follower          ON user_follows(follower_id);
CREATE INDEX idx_user_follows_following         ON user_follows(following_id);
CREATE INDEX idx_posts_user_id                  ON posts(user_id);
CREATE INDEX idx_activity_user_id               ON activities(user_id);
CREATE INDEX idx_activity_target_user_id        ON activities(target_user_id);
CREATE INDEX idx_activity_entity                ON activities(entity_type, entity_id);
CREATE INDEX idx_post_likes_post_id             ON post_likes(post_id);
CREATE INDEX idx_post_likes_user_id             ON post_likes(user_id);
CREATE INDEX idx_saved_posts_user_id            ON saved_posts(user_id);
CREATE INDEX idx_saved_posts_post_id            ON saved_posts(post_id);
CREATE INDEX idx_reposts_user_id                ON reposts(user_id);
CREATE INDEX idx_reposts_post_id                ON reposts(post_id);
CREATE INDEX idx_post_hashtags_post_id          ON post_hashtags(post_id);
CREATE INDEX idx_post_hashtags_hashtag_id       ON post_hashtags(hashtag_id);
CREATE INDEX idx_comments_post_id               ON comments(post_id);
CREATE INDEX idx_comments_user_id               ON comments(user_id);
CREATE INDEX idx_comments_parent_id             ON comments(parent_id);
CREATE INDEX idx_comments_post_parent           ON comments(post_id, parent_id);

-- ============================================================
-- STEP 14: Drop old BIGSERIAL sequences (no longer needed)
-- ============================================================

DROP SEQUENCE IF EXISTS users_id_seq;
DROP SEQUENCE IF EXISTS hashtags_id_seq;
DROP SEQUENCE IF EXISTS refresh_tokens_id_seq;
DROP SEQUENCE IF EXISTS verification_tokens_id_seq;
DROP SEQUENCE IF EXISTS user_devices_id_seq;
DROP SEQUENCE IF EXISTS device_verification_tokens_id_seq;
DROP SEQUENCE IF EXISTS password_reset_tokens_id_seq;
DROP SEQUENCE IF EXISTS user_fcm_tokens_id_seq;
DROP SEQUENCE IF EXISTS user_follows_id_seq;
DROP SEQUENCE IF EXISTS posts_id_seq;
DROP SEQUENCE IF EXISTS activities_id_seq;
DROP SEQUENCE IF EXISTS post_likes_id_seq;
DROP SEQUENCE IF EXISTS saved_posts_id_seq;
DROP SEQUENCE IF EXISTS reposts_id_seq;
DROP SEQUENCE IF EXISTS post_hashtags_id_seq;
DROP SEQUENCE IF EXISTS comments_id_seq;
