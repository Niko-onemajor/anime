package com.example.anime.repository;

import com.example.anime.model.AnimeRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimeRatingRepository extends JpaRepository<AnimeRating, Long> {
    // 根据用户ID和动漫ID查询评分
    Optional<AnimeRating> findByUserIdAndAnimeId(Long userId, Long animeId);

    // 计算动漫的平均评分
    @Query("SELECT AVG(r.rating) FROM AnimeRating r WHERE r.animeId = :animeId")
    Double calculateAverageRating(@Param("animeId") Long animeId);
    
    // 根据用户ID查询所有评分
    @Query("SELECT r FROM AnimeRating r WHERE r.userId = :userId ORDER BY r.createTime DESC")
    List<AnimeRating> findByUserIdOrderByCreateTimeDesc(@Param("userId") Long userId);
    
    // 根据动漫ID删除所有评分
    @Modifying
    @Query("DELETE FROM AnimeRating r WHERE r.animeId = :animeId")
    void deleteByAnimeId(@Param("animeId") Long animeId);
    
    // 根据用户ID删除所有评分
    @Modifying
    @Query("DELETE FROM AnimeRating r WHERE r.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}