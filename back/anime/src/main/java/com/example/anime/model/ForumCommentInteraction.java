package com.example.anime.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "forum_comment_interactions")
public class ForumCommentInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long commentId;
    private Long userId;
    private Integer interactionType; // 1: 点赞, 2: 点踩
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(Integer interactionType) {
        this.interactionType = interactionType;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}