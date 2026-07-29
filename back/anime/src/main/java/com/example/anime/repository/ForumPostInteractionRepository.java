package com.example.anime.repository;

import com.example.anime.model.ForumPostInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ForumPostInteractionRepository extends JpaRepository<ForumPostInteraction, Long> {
    Optional<ForumPostInteraction> findByUserIdAndPostId(Long userId, Long postId);
    int countByPostIdAndInteractionType(Long postId, Integer interactionType);

    // 批量查询所有帖子的互动计数（一次查询替代N次）
    @Query("SELECT f.postId, f.interactionType, COUNT(f) FROM ForumPostInteraction f GROUP BY f.postId, f.interactionType")
    List<Object[]> countGroupByPostIdAndType();
}