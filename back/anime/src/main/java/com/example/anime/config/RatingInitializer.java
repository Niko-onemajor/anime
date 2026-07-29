package com.example.anime.config;

import com.example.anime.model.Anime;
import com.example.anime.repository.AnimeRatingRepository;
import com.example.anime.repository.AnimeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RatingInitializer implements CommandLineRunner {
    
    @Autowired
    private AnimeRepository animeRepository;
    
    @Autowired
    private AnimeRatingRepository animeRatingRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // 获取所有动漫
        List<Anime> animes = animeRepository.findAll();
        
        // 计算并更新每个动漫的平均评分
        for (Anime anime : animes) {
            Double averageRating = animeRatingRepository.calculateAverageRating(anime.getId());
            if (averageRating != null) {
                // 保留一位小数
                Double finalRating = Math.round(averageRating * 10) / 10.0;
                anime.setRating(finalRating);
                animeRepository.save(anime);
            }
        }
        
        log.info("动漫评分初始化完成");
    }
}
