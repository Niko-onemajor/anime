package com.example.anime.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "anime_comments")
public class AnimeComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "like_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer likeCount;

    @Column(name = "dislike_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer dislikeCount;

    // 关联关系
    @ManyToOne
    @JoinColumn(name = "anime_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Anime anime;

    @ManyToOne
    @JoinColumn(name = "author_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "parent_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AnimeComment parent;
}