package com.example.anime.utils;

import com.example.anime.model.Anime;
import com.example.anime.model.AnimeRating;
import com.example.anime.model.Episode;
import com.example.anime.model.Favorite;
import com.example.anime.model.User;
import com.example.anime.model.WatchHistory;
import com.example.anime.repository.AnimeRatingRepository;
import com.example.anime.repository.AnimeRepository;
import com.example.anime.repository.EpisodeRepository;
import com.example.anime.repository.FavoriteRepository;
import com.example.anime.repository.UserRepository;
import com.example.anime.repository.WatchHistoryRepository;
import com.example.anime.service.AnimeRatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Component
public class DataGenerator {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AnimeRepository animeRepository;
    @Autowired
    private AnimeRatingRepository animeRatingRepository;
    @Autowired
    private EpisodeRepository episodeRepository;
    @Autowired
    private WatchHistoryRepository watchHistoryRepository;
    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private AnimeRatingService animeRatingService;

    private final Random random = new Random();

    public void generateData() {
        // 获取所有用户
        List<User> users = userRepository.findAll();
        // 获取所有动漫
        List<Anime> animes = animeRepository.findAll();

        if (users.isEmpty() || animes.isEmpty()) {
            System.out.println("用户或动漫数据为空，无法生成数据");
            return;
        }

        // 为每个用户生成观看记录、收藏和评分，包括管理员账号
        for (User user : users) {
            // 生成观看记录
            generateWatchHistory(user, animes);
            // 生成收藏，控制在5个以内
            generateFavorites(user, animes);
            // 生成评分，控制在5-10个以内
            generateRatings(user, animes);
        }

        // 更新所有动漫的平均评分
        for (Anime anime : animes) {
            animeRatingService.updateAnimeAverageRating(anime.getId());
        }

        System.out.println("数据生成完成");
        System.out.println("动漫平均评分更新完成");
    }

    private void generateWatchHistory(User user, List<Anime> animes) {
        if (animes.isEmpty()) {
            System.out.println("动漫数据为空，无法生成观看记录");
            return;
        }
        
        // 每个用户生成5-10条观看记录
        int watchCount = 5 + random.nextInt(6);
        List<Anime> watchedAnimes = new ArrayList<>();

        for (int i = 0; i < watchCount; i++) {
            // 随机选择一个动漫
            int animeIndex = random.nextInt(animes.size());
            Anime anime = animes.get(animeIndex);
            // 避免重复观看同一部动漫
            if (watchedAnimes.contains(anime)) {
                i--;
                continue;
            }
            watchedAnimes.add(anime);

            // 随机生成观看时间（最近30天内）
            long randomDays = random.nextLong() % 30L;
            if (randomDays < 0) randomDays = -randomDays;
            Date watchTime = new Date(System.currentTimeMillis() - randomDays * 24L * 60L * 60L * 1000L);

            // 获取该动漫的所有集数
            List<Episode> episodes = episodeRepository.findByAnimeIdAndDeletedFalse(anime.getId());
            if (episodes.isEmpty()) {
                continue; // 如果没有集数，跳过
            }

            // 随机选择一个集数
            int episodeIndex = random.nextInt(episodes.size());
            Episode episode = episodes.get(episodeIndex);

            // 检查数据库中是否已经存在该用户对该集数的观看记录
            if (watchHistoryRepository.findByUserIdAndAnimeIdAndEpisodeId(user.getId(), anime.getId(), episode.getId()) != null) {
                i--;
                continue;
            }

            // 创建观看记录
            WatchHistory watchHistory = new WatchHistory();
            watchHistory.setUserId(user.getId());
            watchHistory.setAnimeId(anime.getId());
            watchHistory.setEpisodeId(episode.getId()); // 使用实际的episode_id
            watchHistory.setWatchTime(watchTime);

            watchHistoryRepository.save(watchHistory);
        }
    }

    private void generateFavorites(User user, List<Anime> animes) {
        if (animes.isEmpty()) {
            System.out.println("动漫数据为空，无法生成收藏");
            return;
        }
        
        // 每个用户生成3-5个收藏
        int favoriteCount = 3 + random.nextInt(3);
        List<Anime> favoritedAnimes = new ArrayList<>();

        for (int i = 0; i < favoriteCount; i++) {
            // 随机选择一个动漫
            int animeIndex = random.nextInt(animes.size());
            Anime anime = animes.get(animeIndex);
            // 避免重复收藏同一部动漫
            if (favoritedAnimes.contains(anime)) {
                i--;
                continue;
            }
            
            // 检查数据库中是否已经存在该用户对该动漫的收藏记录
            if (favoriteRepository.findByUserIdAndAnimeId(user.getId(), anime.getId()).isPresent()) {
                i--;
                continue;
            }
            
            favoritedAnimes.add(anime);

            // 创建收藏记录
            Favorite favorite = new Favorite();
            favorite.setUserId(user.getId());
            favorite.setAnimeId(anime.getId());
            favorite.setCreateTime(new Date());

            favoriteRepository.save(favorite);
        }
    }

    private void generateRatings(User user, List<Anime> animes) {
        if (animes.isEmpty()) {
            System.out.println("动漫数据为空，无法生成评分");
            return;
        }
        
        // 每个用户生成5-10个评分
        int ratingCount = 5 + random.nextInt(6);
        List<Anime> ratedAnimes = new ArrayList<>();

        for (int i = 0; i < ratingCount; i++) {
            // 随机选择一个动漫
            int animeIndex = random.nextInt(animes.size());
            Anime anime = animes.get(animeIndex);
            // 避免重复评分同一部动漫
            if (ratedAnimes.contains(anime)) {
                i--;
                continue;
            }
            
            // 检查数据库中是否已经存在该用户对该动漫的评分
            if (animeRatingRepository.findByUserIdAndAnimeId(user.getId(), anime.getId()).isPresent()) {
                i--;
                continue;
            }
            
            ratedAnimes.add(anime);

            // 生成随机评分（2的倍数，2-10分）
            int ratingInt = 2 + (random.nextInt(5) * 2);
            double rating = (double) ratingInt;

            // 创建评分记录
            AnimeRating animeRating = new AnimeRating();
            animeRating.setUserId(user.getId());
            animeRating.setAnimeId(anime.getId());
            animeRating.setRating(rating);
            animeRating.setCreateTime(new Date());

            animeRatingRepository.save(animeRating);
        }
    }
}