package com.example.anime.controller;

import com.example.anime.model.Anime;
import com.example.anime.service.FavoriteService;
import com.example.anime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private UserService userService;
    
    // 添加收藏
    @PostMapping("/add")
    public Map<String, Object> addFavorite(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            Long animeId = Long.parseLong(request.get("animeId").toString());
            
            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();
            
            // 添加收藏
            favoriteService.addFavorite(userId, animeId);
            
            response.put("code", 200);
            response.put("msg", "收藏成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "收藏失败：" + e.getMessage());
        }
        return response;
    }
    
    // 取消收藏
    @PostMapping("/remove")
    public Map<String, Object> removeFavorite(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            Long animeId = Long.parseLong(request.get("animeId").toString());
            
            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();
            
            // 取消收藏
            favoriteService.removeFavorite(userId, animeId);
            
            response.put("code", 200);
            response.put("msg", "取消收藏成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "取消收藏失败：" + e.getMessage());
        }
        return response;
    }
    
    // 获取用户的收藏列表
    @PostMapping("/list")
    public Map<String, Object> getUserFavorites(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            
            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();
            
            // 获取收藏列表
            List<Anime> favorites = favoriteService.getUserFavorites(userId);
            
            response.put("code", 200);
            response.put("data", favorites);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取收藏列表失败：" + e.getMessage());
        }
        return response;
    }
    
    // 检查是否已收藏
    @PostMapping("/check")
    public Map<String, Object> checkFavorite(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            Long animeId = Long.parseLong(request.get("animeId").toString());
            
            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();
            
            // 检查是否已收藏
            boolean isFavorite = favoriteService.isFavorite(userId, animeId);
            
            response.put("code", 200);
            response.put("data", isFavorite);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "检查收藏状态失败：" + e.getMessage());
        }
        return response;
    }
}