package com.example.anime.repository;

import com.example.anime.model.CommentInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentInteractionRepository extends JpaRepository<CommentInteraction, Long> {
    // 根据用户ID和评论ID查询互动记录
    Optional<CommentInteraction> findByUserIdAndCommentId(Long userId, Long commentId);
    
    // 根据评论ID和互动类型统计互动次数
    int countByCommentIdAndInteractionType(Long commentId, Integer interactionType);
    
    // 根据评论ID查询所有互动记录
    List<CommentInteraction> findByCommentId(Long commentId);
}