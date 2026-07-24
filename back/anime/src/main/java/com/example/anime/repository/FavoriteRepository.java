package com.example.anime.repository;

import com.example.anime.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    // 根据用户ID和动漫ID查找收藏
    Optional<Favorite> findByUserIdAndAnimeId(Long userId, Long animeId);
    
    // 根据用户ID查找所有收藏
    List<Favorite> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    // 根据用户ID和动漫ID删除收藏
    void deleteByUserIdAndAnimeId(Long userId, Long animeId);
    
    // 根据动漫ID删除所有收藏
    void deleteByAnimeId(Long animeId);
}