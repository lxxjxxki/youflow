CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE playlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE videos (
    id BIGSERIAL PRIMARY KEY,
    playlist_id BIGINT NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
    youtube_url VARCHAR(500) NOT NULL,
    youtube_id VARCHAR(20) NOT NULL,
    title VARCHAR(500) NOT NULL,
    thumbnail VARCHAR(500),
    position INT NOT NULL DEFAULT 0,
    volume INT NOT NULL DEFAULT 70,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_videos_playlist_id ON videos(playlist_id);
CREATE INDEX idx_playlists_user_id ON playlists(user_id);
