package com.example.anime.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "follows")
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "follower_id", nullable = false)
    private Long followerId;
    
    @Column(name = "followed_id", nullable = false)
    private Long followedId;
    
    @Column(name = "create_time", nullable = false)
    private Date createTime;
    
    // 构造方法
    public Follow() {
    }
    
    public Follow(Long followerId, Long followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
        this.createTime = new Date();
    }
    
    // getter和setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getFollowerId() {
        return followerId;
    }
    
    public void setFollowerId(Long followerId) {
        this.followerId = followerId;
    }
    
    public Long getFollowedId() {
        return followedId;
    }
    
    public void setFollowedId(Long followedId) {
        this.followedId = followedId;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}