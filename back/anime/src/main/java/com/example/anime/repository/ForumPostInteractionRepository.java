package com.example.anime.repository;

import com.example.anime.model.ForumPostInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumPostInteractionRepository extends JpaRepository<ForumPostInteraction, Long> {
    Optional<ForumPostInteraction> findByUserIdAndPostId(Long userId, Long postId);
    int countByPostIdAndInteractionType(Long postId, Integer interactionType);
}