package com.example.anime.repository;

import com.example.anime.model.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    // 根据动漫ID获取所有集数
    List<Episode> findByAnimeIdAndDeletedFalse(Long animeId);
    
    // 根据动漫ID和集数获取特定集数
    Episode findByAnimeIdAndEpisodeNumberAndDeletedFalse(Long animeId, Integer episodeNumber);
    
    // 查询所有未删除的集数
    List<Episode> findByDeletedFalse();
    
    // 查询所有已删除的集数
    List<Episode> findByDeletedTrue();
    
    // 根据ID查询未删除的集数
    Episode findByIdAndDeletedFalse(Long id);
}
