package com.example.anime.controller;

import com.example.anime.model.Anime;
import com.example.anime.service.AnimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/anime")
public class AnimeController {
    @Autowired
    private AnimeService animeService;

    // 获取所有动漫
    @GetMapping("/list")
    public List<Anime> getAnimes() {
        return animeService.getAllAnimes();
    }

    // 根据ID获取动漫
    @GetMapping("/detail/{id}")
    public Anime getAnime(@PathVariable Long id) {
        return animeService.getAnimeById(id);
    }

    // 根据年份获取动漫
    @GetMapping("/year/{year}")
    public List<Anime> getAnimesByYear(@PathVariable String year) {
        return animeService.getAnimesByYear(year);
    }

    // 根据首字母获取动漫
    @GetMapping("/letter/{letter}")
    public List<Anime> getAnimesByLetter(@PathVariable String letter) {
        return animeService.getAnimesByLetter(letter);
    }

    // 按评分排序获取动漫
    @GetMapping("/rating")
    public List<Anime> getAnimesByRating() {
        return animeService.getAnimesByRating();
    }

    // 按观看次数排序获取热门动漫
    @GetMapping("/popular")
    public List<Map<String, Object>> getPopularAnimes() {
        return animeService.getPopularAnimesByWatchCount();
    }
    
    // 获取周榜
    @GetMapping("/ranking/weekly")
    public List<Map<String, Object>> getWeeklyRanking() {
        return animeService.getWeeklyRanking();
    }
    
    // 获取月榜
    @GetMapping("/ranking/monthly")
    public List<Map<String, Object>> getMonthlyRanking() {
        return animeService.getMonthlyRanking();
    }
    
    // 获取年榜
    @GetMapping("/ranking/yearly")
    public List<Map<String, Object>> getYearlyRanking() {
        return animeService.getYearlyRanking();
    }

    // 获取动漫的观看次数
    @GetMapping("/watch-count/{id}")
    public Map<String, Object> getAnimeWatchCount(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        int watchCount = animeService.getAnimeWatchCount(id);
        response.put("watchCount", watchCount);
        return response;
    }

    // 按年份排序获取动漫
    @GetMapping("/year")
    public List<Anime> getAnimesByYear() {
        return animeService.getAnimesByYear();
    }

    // 搜索动漫
    @PostMapping("/search")
    public List<Anime> searchAnimes(@RequestBody Map<String, String> request) {
        String keyword = request.get("keyword");
        return animeService.searchAnimes(keyword);
    }

    // 保存动漫
    @PostMapping("/save")
    public Anime saveAnime(@RequestBody Anime anime) {
        return animeService.saveAnime(anime);
    }

    // 删除动漫
    @DeleteMapping("/delete/{id}")
    public void deleteAnime(@PathVariable Long id) {
        animeService.deleteAnime(id);
    }
    
    // 将所有动漫设置为上架状态
    @PostMapping("/publishAll")
    public Map<String, Object> publishAllAnimes() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Anime> animes = animeService.findAll();
            for (Anime anime : animes) {
                anime.setStatus(1);
                animeService.save(anime);
            }
            response.put("code", 200);
            response.put("msg", "所有动漫已设置为上架状态");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "操作失败");
        }
        return response;
    }
}