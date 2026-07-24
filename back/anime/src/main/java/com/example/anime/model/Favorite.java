package com.example.anime.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "favorites")
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "anime_id", nullable = false)
    private Long animeId;
    
    @Column(name = "create_time", nullable = false)
    private Date createTime;
    
    // 构造方法
    public Favorite() {
    }
    
    public Favorite(Long userId, Long animeId) {
        this.userId = userId;
        this.animeId = animeId;
        this.createTime = new Date();
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
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}