package com.example.anime.repository;

import com.example.anime.model.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    // 根据用户ID获取观看记录，按观看时间倒序排列
    List<WatchHistory> findByUserIdOrderByWatchTimeDesc(Long userId);

    // 根据用户ID和动漫ID获取观看记录
    List<WatchHistory> findByUserIdAndAnimeIdOrderByWatchTimeDesc(Long userId, Long animeId);
    
    // 根据用户ID、动漫ID和集数ID查找观看记录
    WatchHistory findByUserIdAndAnimeIdAndEpisodeId(Long userId, Long animeId, Long episodeId);
    
    // 根据集数ID获取所有观看记录
    List<WatchHistory> findByEpisodeId(Long episodeId);
    
    // 根据动漫ID获取所有观看记录
    List<WatchHistory> findByAnimeId(Long animeId);
    
    // 根据用户ID获取所有观看记录
    List<WatchHistory> findByUserId(Long userId);
}