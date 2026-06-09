-- US-081: System banners may carry an optional click-through link and a banner type.
-- These columns back the SystemBannerResponse DTO returned by GET /api/v1/feed/banners.
ALTER TABLE feed_posts ADD COLUMN link VARCHAR(1000);
ALTER TABLE feed_posts ADD COLUMN banner_type VARCHAR(30);
