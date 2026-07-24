package com.example.anime.service;

import com.example.anime.model.Anime;
import com.example.anime.model.AnimeComment;
import com.example.anime.model.Favorite;
import com.example.anime.model.WatchHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    @Autowired
    private WatchHistoryService watchHistoryService;

    @Autowired
    private AnimeService animeService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private AnimeCommentService animeCommentService;

    // 基于用户多维度数据的推荐
    public List<Anime> getRecommendationsByWatchHistory(Long userId) {
        // 获取用户观看历史
        List<WatchHistory> watchHistories = watchHistoryService.getUserWatchHistory(userId);
        // 获取用户收藏
        List<Favorite> favorites = favoriteService.getUserFavoriteEntities(userId);
        // 获取用户评论
        List<AnimeComment> comments = animeCommentService.getCommentsByAuthorId(userId);
        
        if (watchHistories.isEmpty() && favorites.isEmpty() && comments.isEmpty()) {
            // 如果没有任何用户行为数据，返回热门动漫
            return getPopularAnime();
        }

        // 计算动漫观看次数
        Map<Long, Integer> animeWatchCount = new HashMap<>();
        for (WatchHistory history : watchHistories) {
            animeWatchCount.put(history.getAnimeId(), animeWatchCount.getOrDefault(history.getAnimeId(), 0) + 1);
        }

        // 计算用户收藏的动漫
        Set<Long> favoritedAnimeIds = favorites.stream()
                .map(Favorite::getAnimeId)
                .collect(Collectors.toSet());

        // 计算用户评论的动漫
        Set<Long> commentedAnimeIds = comments.stream()
                .map(AnimeComment::getAnimeId)
                .collect(Collectors.toSet());

        // 合并用户交互过的动漫ID
        Set<Long> interactedAnimeIds = new HashSet<>(animeWatchCount.keySet());
        interactedAnimeIds.addAll(favoritedAnimeIds);
        interactedAnimeIds.addAll(commentedAnimeIds);

        // 获取所有动漫
        List<Anime> allAnime = animeService.findAll();
        
        // 过滤掉用户已经交互过的动漫
        List<Anime> unwatchedAnime = allAnime.stream()
                .filter(anime -> !interactedAnimeIds.contains(anime.getId()))
                .collect(Collectors.toList());

        // 如果没有未交互的动漫，返回最多5个动漫
        if (unwatchedAnime.isEmpty()) {
            if (allAnime.size() > 5) {
                return allAnime.subList(0, 5);
            }
            return allAnime;
        }

        // 基于用户多维度数据的推荐算法
        // 1. 计算用户偏好的标签（加权计算）
        Map<String, Double> userGenrePreferences = new HashMap<>();
        
        // 观看历史权重
        for (Map.Entry<Long, Integer> entry : animeWatchCount.entrySet()) {
            Long animeId = entry.getKey();
            int watchCount = entry.getValue();
            Anime anime = animeService.findById(animeId);
            if (anime != null && anime.getGenre() != null) {
                String[] genres = anime.getGenre().split(",");
                for (String genre : genres) {
                    genre = genre.trim();
                    if (!genre.isEmpty()) {
                        double weight = watchCount * 1.0; // 观看次数作为权重
                        userGenrePreferences.put(genre, userGenrePreferences.getOrDefault(genre, 0.0) + weight);
                    }
                }
            }
        }
        
        // 收藏权重（更高）
        for (Long animeId : favoritedAnimeIds) {
            Anime anime = animeService.findById(animeId);
            if (anime != null && anime.getGenre() != null) {
                String[] genres = anime.getGenre().split(",");
                for (String genre : genres) {
                    genre = genre.trim();
                    if (!genre.isEmpty()) {
                        double weight = 3.0; // 收藏权重更高
                        userGenrePreferences.put(genre, userGenrePreferences.getOrDefault(genre, 0.0) + weight);
                    }
                }
            }
        }
        
        // 评论权重
        for (Long animeId : commentedAnimeIds) {
            Anime anime = animeService.findById(animeId);
            if (anime != null && anime.getGenre() != null) {
                String[] genres = anime.getGenre().split(",");
                for (String genre : genres) {
                    genre = genre.trim();
                    if (!genre.isEmpty()) {
                        double weight = 2.0; // 评论权重
                        userGenrePreferences.put(genre, userGenrePreferences.getOrDefault(genre, 0.0) + weight);
                    }
                }
            }
        }

        // 2. 计算每部未交互动漫与用户偏好的匹配度
        List<Map.Entry<Anime, Double>> animeScores = new ArrayList<>();
        for (Anime anime : unwatchedAnime) {
            if (anime.getGenre() != null) {
                String[] genres = anime.getGenre().split(",");
                double score = 0.0;
                
                // 标签匹配度
                for (String genre : genres) {
                    genre = genre.trim();
                    if (!genre.isEmpty() && userGenrePreferences.containsKey(genre)) {
                        score += userGenrePreferences.get(genre);
                    }
                }
                
                // 加入动漫评分作为因素（0.3权重）
                if (anime.getRating() != null) {
                    score += anime.getRating() * 0.3;
                }
                
                animeScores.add(new AbstractMap.SimpleEntry<>(anime, score));
            }
        }

        // 3. 按匹配度排序
        animeScores.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // 4. 提取推荐结果
        List<Anime> recommendations = new ArrayList<>();
        for (Map.Entry<Anime, Double> entry : animeScores) {
            recommendations.add(entry.getKey());
            if (recommendations.size() >= 5) {
                break;
            }
        }

        // 5. 如果推荐结果不足5个，补充热门动漫
        if (recommendations.size() < 5) {
            List<Anime> popularAnime = getPopularAnime();
            for (Anime anime : popularAnime) {
                if (!recommendations.contains(anime) && !interactedAnimeIds.contains(anime.getId())) {
                    recommendations.add(anime);
                    if (recommendations.size() >= 5) {
                        break;
                    }
                }
            }
        }

        return recommendations;
    }

    // 获取热门动漫（基于多维度数据）
    public List<Anime> getPopularAnime() {
        // 获取所有观看历史
        List<WatchHistory> allWatchHistories = watchHistoryService.getAllWatchHistory();
        // 获取所有收藏
        List<Favorite> allFavorites = favoriteService.getAllFavorites();
        // 获取所有评论
        List<AnimeComment> allComments = animeCommentService.getAllComments();
        
        // 计算动漫热度分数
        Map<Long, Double> animePopularityScores = new HashMap<>();
        
        // 观看次数权重
        for (WatchHistory history : allWatchHistories) {
            Long animeId = history.getAnimeId();
            animePopularityScores.put(animeId, animePopularityScores.getOrDefault(animeId, 0.0) + 1.0);
        }
        
        // 收藏次数权重（更高）
        for (Favorite favorite : allFavorites) {
            Long animeId = favorite.getAnimeId();
            animePopularityScores.put(animeId, animePopularityScores.getOrDefault(animeId, 0.0) + 3.0);
        }
        
        // 评论次数权重
        for (AnimeComment comment : allComments) {
            Long animeId = comment.getAnimeId();
            animePopularityScores.put(animeId, animePopularityScores.getOrDefault(animeId, 0.0) + 2.0);
        }

        // 按热度分数排序
        List<Long> popularAnimeIds = animePopularityScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 获取热门动漫
        List<Anime> popularAnime = new ArrayList<>();
        for (Long animeId : popularAnimeIds) {
            Anime anime = animeService.findById(animeId);
            if (anime != null) {
                popularAnime.add(anime);
            }
        }

        // 如果热门动漫不足5个，补充其他动漫
        if (popularAnime.size() < 5) {
            List<Anime> allAnime = animeService.findAll();
            Set<Long> popularAnimeIdSet = popularAnimeIds.stream().collect(Collectors.toSet());
            
            // 按评分排序补充
            allAnime.sort((a, b) -> {
                double ratingA = a.getRating() != null ? a.getRating() : 0;
                double ratingB = b.getRating() != null ? b.getRating() : 0;
                return Double.compare(ratingB, ratingA);
            });
            
            for (Anime anime : allAnime) {
                if (!popularAnimeIdSet.contains(anime.getId())) {
                    popularAnime.add(anime);
                    if (popularAnime.size() >= 5) {
                        break;
                    }
                }
            }
        }

        // 如果仍然没有推荐动漫，直接返回所有动漫的前5个
        if (popularAnime.isEmpty()) {
            List<Anime> allAnime = animeService.findAll();
            if (!allAnime.isEmpty()) {
                if (allAnime.size() > 5) {
                    return allAnime.subList(0, 5);
                }
                return allAnime;
            }
        }

        // 确保最多返回5个动漫
        if (popularAnime.size() > 5) {
            return popularAnime.subList(0, 5);
        }

        return popularAnime;
    }
}
