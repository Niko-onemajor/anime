package com.example.anime.repository;

import com.example.anime.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Follow findByFollowerIdAndFollowedId(Long followerId, Long followedId);
    long countByFollowedId(Long followedId);
    long countByFollowerId(Long followerId);
    List<Follow> findByFollowerId(Long followerId);
    List<Follow> findByFollowedId(Long followedId);
    void deleteByFollowerIdAndFollowedId(Long followerId, Long followedId);
    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);
}