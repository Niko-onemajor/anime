package com.example.anime.controller;

import com.example.anime.model.Anime;
import com.example.anime.service.RecommendationService;
import com.example.anime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {
    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserService userService;

    // 获取个性化推荐
    @PostMapping("/personalized")
    public Map<String, Object> getPersonalizedRecommendations(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");

            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();

            // 获取个性化推荐
            List<Anime> recommendations = recommendationService.getRecommendationsByWatchHistory(userId);

            // 处理推荐数据
            List<Map<String, Object>> processedRecommendations = new ArrayList<>();
            for (Anime anime : recommendations) {
                Map<String, Object> animeMap = new HashMap<>();
                animeMap.put("id", anime.getId());
                animeMap.put("title", anime.getTitle());
                animeMap.put("image", anime.getImage());
                animeMap.put("category", anime.getGenre());
                animeMap.put("description", anime.getDescription());
                processedRecommendations.add(animeMap);
            }

            response.put("code", 200);
            response.put("data", processedRecommendations);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取推荐失败：" + e.getMessage());
        }
        return response;
    }

    // 获取热门动漫
    @GetMapping("/popular")
    public Map<String, Object> getPopularAnime() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 获取热门动漫
            List<Anime> popularAnime = recommendationService.getPopularAnime();

            // 处理热门动漫数据
            List<Map<String, Object>> processedPopularAnime = new ArrayList<>();
            for (Anime anime : popularAnime) {
                Map<String, Object> animeMap = new HashMap<>();
                animeMap.put("id", anime.getId());
                animeMap.put("title", anime.getTitle());
                animeMap.put("image", anime.getImage());
                animeMap.put("category", anime.getGenre());
                animeMap.put("description", anime.getDescription());
                processedPopularAnime.add(animeMap);
            }

            response.put("code", 200);
            response.put("data", processedPopularAnime);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取热门动漫失败：" + e.getMessage());
        }
        return response;
    }
}
