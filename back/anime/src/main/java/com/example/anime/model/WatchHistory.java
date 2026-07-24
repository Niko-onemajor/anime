package com.example.anime.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "watch_history")
public class WatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    @Column(name = "episode_id", nullable = false)
    private Long episodeId;

    @Column(name = "watch_time", nullable = false)
    private Date watchTime;

    // 构造方法
    public WatchHistory() {
    }

    public WatchHistory(Long userId, Long animeId, Long episodeId, Date watchTime) {
        this.userId = userId;
        this.animeId = animeId;
        this.episodeId = episodeId;
        this.watchTime = watchTime;
    }

    // getter和setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Long animeId) {
        this.animeId = animeId;
    }

    public Long getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(Long episodeId) {
        this.episodeId = episodeId;
    }

    public Date getWatchTime() {
        return watchTime;
    }

    public void setWatchTime(Date watchTime) {
        this.watchTime = watchTime;
    }
}