package com.example.anime.service;

import com.example.anime.model.Episode;
import com.example.anime.repository.EpisodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EpisodeService {
    @Autowired
    private EpisodeRepository episodeRepository;
    
    @Autowired
    private WatchHistoryService watchHistoryService;

    // 根据动漫ID获取所有集数（非删除）
    public List<Episode> getEpisodesByAnimeId(Long animeId) {
        return episodeRepository.findByAnimeIdAndDeletedFalse(animeId);
    }

    // 根据动漫ID和集数获取特定集数（非删除）
    public Episode getEpisodeByAnimeIdAndEpisodeNumber(Long animeId, Integer episodeNumber) {
        return episodeRepository.findByAnimeIdAndEpisodeNumberAndDeletedFalse(animeId, episodeNumber);
    }

    // 保存集数
    public Episode save(Episode episode) {
        return episodeRepository.save(episode);
    }

    // 删除集数（逻辑删除）
    public void delete(Long id) {
        Episode episode = episodeRepository.findByIdAndDeletedFalse(id);
        if (episode != null) {
            episode.setDeleted(true);
            episode.setDeletedAt(new java.util.Date());
            episodeRepository.save(episode);
        }
    }

    // 根据ID获取集数（非删除）
    public Episode findById(Long id) {
        return episodeRepository.findByIdAndDeletedFalse(id);
    }
    
    // 获取所有已删除的集数
    public List<Episode> findAllDeleted() {
        return episodeRepository.findByDeletedTrue();
    }
    
    // 恢复已删除的集数
    public void restore(Long id) {
        Episode episode = episodeRepository.findById(id).orElse(null);
        if (episode != null) {
            episode.setDeleted(false);
            episode.setDeletedAt(null);
            episodeRepository.save(episode);
        }
    }
    
    // 彻底删除集数
    public void hardDelete(Long id) {
        // 先删除相关的观看记录
        watchHistoryService.deleteByEpisodeId(id);
        // 再删除集数
        episodeRepository.deleteById(id);
    }
}
