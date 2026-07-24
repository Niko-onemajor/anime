package com.example.anime.controller;

import com.example.anime.model.Episode;
import com.example.anime.service.EpisodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/episode")
public class EpisodeController {
    @Autowired
    private EpisodeService episodeService;

    // 根据动漫ID获取所有集数
    @GetMapping("/anime/{animeId}")
    public Map<String, Object> getEpisodesByAnimeId(@PathVariable Long animeId) {
        Map<String, Object> response = new HashMap<>();
        List<Episode> episodes = episodeService.getEpisodesByAnimeId(animeId);
        response.put("code", 200);
        response.put("msg", "获取成功");
        response.put("data", episodes);
        return response;
    }

    // 根据动漫ID和集数获取特定集数
    @GetMapping("/anime/{animeId}/episode/{episodeNumber}")
    public Map<String, Object> getEpisodeByAnimeIdAndEpisodeNumber(@PathVariable Long animeId, @PathVariable Integer episodeNumber) {
        Map<String, Object> response = new HashMap<>();
        Episode episode = episodeService.getEpisodeByAnimeIdAndEpisodeNumber(animeId, episodeNumber);
        if (episode != null) {
            response.put("code", 200);
            response.put("msg", "获取成功");
            response.put("data", episode);
        } else {
            response.put("code", 404);
            response.put("msg", "集数不存在");
        }
        return response;
    }
}
