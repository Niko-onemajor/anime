package com.example.anime.repository;

import com.example.anime.model.ForumCommentInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumCommentInteractionRepository extends JpaRepository<ForumCommentInteraction, Long> {
    // 根据用户ID和评论ID查询互动记录
    Optional<ForumCommentInteraction> findByUserIdAndCommentId(Long userId, Long commentId);
    
    // 根据评论ID和互动类型统计互动次数
    int countByCommentIdAndInteractionType(Long commentId, Integer interactionType);
}