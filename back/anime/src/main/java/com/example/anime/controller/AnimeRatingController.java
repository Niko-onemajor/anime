package com.example.anime.controller;

import com.example.anime.model.Anime;
import com.example.anime.model.AnimeRating;
import com.example.anime.service.AnimeRatingService;
import com.example.anime.service.AnimeService;
import com.example.anime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/anime/rating")
public class AnimeRatingController {
    @Autowired
    private AnimeRatingService animeRatingService;

    @Autowired
    private UserService userService;

    @Autowired
    private AnimeService animeService;

    // 提交评分
    @PostMapping("/submit")
    public Map<String, Object> submitRating(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = Long.parseLong(request.get("animeId").toString());
            String username = (String) request.get("username");
            Double rating = Double.parseDouble(request.get("rating").toString());

            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();

            // 保存评分
            animeRatingService.saveRating(animeId, userId, rating);

            // 获取更新后的动漫信息
            Anime anime = animeService.getAnimeById(animeId);

            response.put("code", 200);
            response.put("msg", "评分成功");
            response.put("data", anime);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "评分失败：" + e.getMessage());
        }
        return response;
    }

    // 获取用户对动漫的评分
    @PostMapping("/user")
    public Map<String, Object> getUserRating(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = Long.parseLong(request.get("animeId").toString());
            String username = (String) request.get("username");

            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();

            // 获取用户评分
            AnimeRating rating = animeRatingService.getUserRating(animeId, userId).orElse(null);

            response.put("code", 200);
            response.put("data", rating);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取评分失败：" + e.getMessage());
        }
        return response;
    }
    
    // 获取用户的所有评分
    @PostMapping("/user/list")
    public Map<String, Object> getUserRatings(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");

            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();

            // 获取用户的所有评分
            List<AnimeRating> ratings = animeRatingService.getUserRatings(userId);

            response.put("code", 200);
            response.put("data", ratings);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取评分列表失败：" + e.getMessage());
        }
        return response;
    }
}