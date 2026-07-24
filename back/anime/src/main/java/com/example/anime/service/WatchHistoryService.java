package com.example.anime.service;

import com.example.anime.model.WatchHistory;
import com.example.anime.repository.WatchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class WatchHistoryService {
    @Autowired
    private WatchHistoryRepository watchHistoryRepository;

    // 保存观看记录
    public WatchHistory saveWatchHistory(Long userId, Long animeId, Long episodeId) {
        // 每次观看都创建新的记录，以增加观看次数
        WatchHistory watchHistory = new WatchHistory(userId, animeId, episodeId, new Date());
        return watchHistoryRepository.save(watchHistory);
    }

    // 获取用户的观看记录
    public List<WatchHistory> getUserWatchHistory(Long userId) {
        return watchHistoryRepository.findByUserIdOrderByWatchTimeDesc(userId);
    }

    // 获取用户对特定动漫的观看记录
    public List<WatchHistory> getUserAnimeWatchHistory(Long userId, Long animeId) {
        return watchHistoryRepository.findByUserIdAndAnimeIdOrderByWatchTimeDesc(userId, animeId);
    }

    // 获取所有用户的观看记录
    public List<WatchHistory> getAllWatchHistory() {
        return watchHistoryRepository.findAll();
    }

    // 根据用户ID获取观看记录（用于管理员）
    public List<WatchHistory> getByUserId(Long userId) {
        return getUserWatchHistory(userId);
    }

    // 根据ID删除观看记录
    public void deleteById(Long id) {
        watchHistoryRepository.deleteById(id);
    }
    
    // 根据集数ID删除所有相关的观看记录
    public void deleteByEpisodeId(Long episodeId) {
        List<WatchHistory> histories = watchHistoryRepository.findByEpisodeId(episodeId);
        for (WatchHistory history : histories) {
            watchHistoryRepository.delete(history);
        }
    }
    
    // 根据动漫ID删除所有相关的观看记录
    public void deleteByAnimeId(Long animeId) {
        List<WatchHistory> histories = watchHistoryRepository.findByAnimeId(animeId);
        for (WatchHistory history : histories) {
            watchHistoryRepository.delete(history);
        }
    }
    
    // 根据用户ID删除所有相关的观看记录
    public void deleteByUserId(Long userId) {
        List<WatchHistory> histories = watchHistoryRepository.findByUserId(userId);
        for (WatchHistory history : histories) {
            watchHistoryRepository.delete(history);
        }
    }
}