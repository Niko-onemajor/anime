package com.example.anime.repository;

import com.example.anime.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 根据帖子ID查询评论
    List<Comment> findByPostId(Long postId);

    // 根据用户ID查询评论
    List<Comment> findByAuthorId(Long authorId);

    // 根据父评论ID查询回复
    List<Comment> findByParentId(Long parentId);

    // 根据帖子ID和parentId为null查询顶级评论
    List<Comment> findByPostIdAndParentIdIsNull(Long postId);

}
