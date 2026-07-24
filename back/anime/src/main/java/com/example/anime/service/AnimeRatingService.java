package com.example.anime.service;

import com.example.anime.model.Anime;
import com.example.anime.model.AnimeRating;
import com.example.anime.repository.AnimeRatingRepository;
import com.example.anime.repository.AnimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AnimeRatingService {
    @Autowired
    private AnimeRatingRepository animeRatingRepository;

    @Autowired
    private AnimeRepository animeRepository;

    // 保存或更新评分
    public AnimeRating saveRating(Long animeId, Long userId, Double rating) {
        // 查找是否已经存在评分
        Optional<AnimeRating> existingRating = animeRatingRepository.findByUserIdAndAnimeId(userId, animeId);

        AnimeRating animeRating;
        if (existingRating.isPresent()) {
            // 更新现有评分
            animeRating = existingRating.get();
            animeRating.setRating(rating);
            animeRating.setCreateTime(new Date());
        } else {
            // 创建新评分
            animeRating = new AnimeRating();
            animeRating.setAnimeId(animeId);
            animeRating.setUserId(userId);
            animeRating.setRating(rating);
            animeRating.setCreateTime(new Date());
        }

        // 保存评分
        AnimeRating savedRating = animeRatingRepository.save(animeRating);

        // 更新动漫的平均评分
        updateAnimeAverageRating(animeId);

        return savedRating;
    }

    // 获取用户对动漫的评分
    public Optional<AnimeRating> getUserRating(Long animeId, Long userId) {
        return animeRatingRepository.findByUserIdAndAnimeId(userId, animeId);
    }
    
    // 获取用户的所有评分
    public List<AnimeRating> getUserRatings(Long userId) {
        return animeRatingRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    // 根据用户ID获取评分列表（用于管理员）
    public List<AnimeRating> getByUserId(Long userId) {
        return getUserRatings(userId);
    }

    // 根据ID删除评分
    public void deleteById(Long id) {
        animeRatingRepository.deleteById(id);
    }

    // 计算并更新动漫的平均评分
    public void updateAnimeAverageRating(Long animeId) {
        Double averageRating = animeRatingRepository.calculateAverageRating(animeId);
        if (averageRating != null) {
            // 保留一位小数
            Double finalRating = Math.round(averageRating * 10) / 10.0;
            
            Optional<Anime> animeOptional = animeRepository.findById(animeId);
            if (animeOptional.isPresent()) {
                Anime anime = animeOptional.get();
                anime.setRating(finalRating);
                animeRepository.save(anime);
            }
        }
    }
    
    // 根据动漫ID删除所有评分
    public void deleteByAnimeId(Long animeId) {
        animeRatingRepository.deleteByAnimeId(animeId);
    }
    
    // 根据用户ID删除所有评分
    @javax.transaction.Transactional
    public void deleteByUserId(Long userId) {
        animeRatingRepository.deleteByUserId(userId);
    }
}