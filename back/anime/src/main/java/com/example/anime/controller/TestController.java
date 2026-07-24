package com.example.anime.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/resources")
    public Map<String, Object> testResources() {
        Map<String, Object> response = new HashMap<>();
        
        // 测试资源访问URL
        response.put("videoUrl", "http://localhost:8080/videos/giant/giant1.mp4");
        response.put("avatarUrl", "http://localhost:8080/avatars/avatar1.webp");
        response.put("coverUrl", "http://localhost:8080/covers/giant.webp");
        
        response.put("code", 200);
        response.put("msg", "测试资源访问URL");
        
        return response;
    }
}
