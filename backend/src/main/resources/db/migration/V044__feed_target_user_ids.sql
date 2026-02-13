-- Add target_user_ids array to feed_posts for targeted feed posts
-- NULL means visible to all (default), non-null means only these users see the post
ALTER TABLE feed_posts ADD COLUMN target_user_ids UUID[];
