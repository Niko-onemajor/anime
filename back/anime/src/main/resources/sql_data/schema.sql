-- 删除现有的表（如果存在）
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS comment_interactions;
DROP TABLE IF EXISTS forum_comment_interactions;
DROP TABLE IF EXISTS anime_comments;
DROP TABLE IF EXISTS anime_ratings;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS watch_history;
DROP TABLE IF EXISTS episodes;
DROP TABLE IF EXISTS animes;
DROP TABLE IF EXISTS users;

-- 创建用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role ENUM('user', 'admin') NOT NULL DEFAULT 'user',
    email VARCHAR(100),
    birthday DATE,
    avatar VARCHAR(255),
    gender VARCHAR(10),
    region VARCHAR(100),
    signature VARCHAR(255),
    favorite VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at DATETIME
);

-- 创建动漫表
CREATE TABLE animes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    image VARCHAR(255),
    year YEAR,
    genre VARCHAR(100),
    rating DECIMAL(3,1),
    letter VARCHAR(1),
    status INT NOT NULL DEFAULT 1,
    view_count INT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at DATETIME,
    INDEX idx_title (title),
    INDEX idx_genre (genre),
    INDEX idx_rating (rating)
);

-- 创建收藏表
CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    anime_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL,
    UNIQUE KEY unique_user_anime (user_id, anime_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (anime_id) REFERENCES animes(id)
);

-- 创建集数表
CREATE TABLE episodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    anime_id BIGINT NOT NULL,
    episode_number INT NOT NULL,
    video_url VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at DATETIME,
    FOREIGN KEY (anime_id) REFERENCES animes(id)
);

-- 创建帖子表
CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT,
    author_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL,
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    FOREIGN KEY (author_id) REFERENCES users(id),
    INDEX idx_title (title),
    INDEX idx_author_id (author_id),
    INDEX idx_create_time (create_time)
);

-- 创建评论表
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    create_time DATETIME NOT NULL,
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);

-- 创建动漫评分表
CREATE TABLE anime_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    anime_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating DOUBLE NOT NULL,
    create_time DATETIME NOT NULL,
    UNIQUE KEY unique_user_anime (user_id, anime_id),
    FOREIGN KEY (anime_id) REFERENCES animes(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 创建动漫评论表
CREATE TABLE anime_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    anime_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    create_time DATETIME NOT NULL,
    parent_id BIGINT,
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    FOREIGN KEY (anime_id) REFERENCES animes(id),
    FOREIGN KEY (author_id) REFERENCES users(id),
    FOREIGN KEY (parent_id) REFERENCES anime_comments(id)
);

-- 创建评论互动表
CREATE TABLE comment_interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    interaction_type INT NOT NULL, -- 1: 点赞, 2: 点踩
    create_time DATETIME NOT NULL,
    UNIQUE KEY unique_user_comment (user_id, comment_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (comment_id) REFERENCES anime_comments(id)
);

-- 创建论坛评论互动表
CREATE TABLE forum_comment_interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    interaction_type INT NOT NULL, -- 1: 点赞, 2: 点踩
    create_time DATETIME NOT NULL,
    UNIQUE KEY unique_user_forum_comment (user_id, comment_id),
    FOREIGN KEY (comment_id) REFERENCES comments(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 创建论坛帖子互动表
CREATE TABLE forum_post_interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    interaction_type INTEGER NOT NULL, -- 1: 点赞, 2: 点踩
    create_time DATETIME NOT NULL,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY unique_user_post (user_id, post_id)
);

-- 创建观看记录表
CREATE TABLE watch_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    anime_id BIGINT NOT NULL,
    episode_id BIGINT NOT NULL,
    watch_time DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (anime_id) REFERENCES animes(id),
    FOREIGN KEY (episode_id) REFERENCES episodes(id),
    INDEX idx_user_anime (user_id, anime_id),
    INDEX idx_user_watch_time (user_id, watch_time),
    INDEX idx_anime_id (anime_id)
);