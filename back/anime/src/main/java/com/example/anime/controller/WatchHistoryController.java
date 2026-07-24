package com.example.anime.controller;

import com.example.anime.model.WatchHistory;
import com.example.anime.service.AnimeService;
import com.example.anime.service.EpisodeService;
import com.example.anime.service.UserService;
import com.example.anime.service.WatchHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watch-history")
public class WatchHistoryController {
    @Autowired
    private WatchHistoryService watchHistoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private AnimeService animeService;

    @Autowired
    private EpisodeService episodeService;

    // 记录观看历史
    @PostMapping("/add")
    public Map<String, Object> addWatchHistory(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            Long animeId = Long.parseLong(request.get("animeId").toString());
            Long episodeId = Long.parseLong(request.get("episodeId").toString());

            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();

            // 保存观看记录
            WatchHistory savedHistory = watchHistoryService.saveWatchHistory(userId, animeId, episodeId);

            response.put("code", 200);
            response.put("msg", "观看记录添加成功");
            response.put("data", savedHistory);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "观看记录添加失败：" + e.getMessage());
        }
        return response;
    }

    // 获取用户的观看记录
    @PostMapping("/list")
    public Map<String, Object> getUserWatchHistory(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");

            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();

            // 获取观看记录
            List<WatchHistory> watchHistories = watchHistoryService.getUserWatchHistory(userId);

            // 处理观看记录数据，添加动漫和集数信息，并去重（同一动漫只保留最新观看的记录）
            Map<String, Map<String, Object>> uniqueHistories = new HashMap<>();
            for (WatchHistory history : watchHistories) {
                // 生成唯一键：只使用动漫ID
                String key = history.getAnimeId().toString();
                
                // 检查是否已存在该动漫的记录
                if (!uniqueHistories.containsKey(key) || 
                    history.getWatchTime().after((Date) uniqueHistories.get(key).get("watchTime"))) {
                    // 如果不存在或当前记录的观看时间更新，则替换为当前记录
                    Map<String, Object> historyMap = new HashMap<>();
                    historyMap.put("id", history.getId());
                    historyMap.put("watchTime", history.getWatchTime());

                    // 添加动漫信息
                    try {
                        com.example.anime.model.Anime anime = animeService.findById(history.getAnimeId());
                        if (anime != null) {
                            Map<String, Object> animeMap = new HashMap<>();
                            animeMap.put("id", anime.getId());
                            animeMap.put("title", anime.getTitle());
                            animeMap.put("image", anime.getImage());
                            historyMap.put("anime", animeMap);
                        }
                    } catch (Exception e) {
                        // 忽略动漫信息获取失败的情况
                    }

                    // 添加集数信息
                    try {
                        com.example.anime.model.Episode episode = episodeService.findById(history.getEpisodeId());
                        if (episode != null) {
                            Map<String, Object> episodeMap = new HashMap<>();
                            episodeMap.put("id", episode.getId());
                            episodeMap.put("episodeNumber", episode.getEpisodeNumber());
                            historyMap.put("episode", episodeMap);
                        }
                    } catch (Exception e) {
                        // 忽略集数信息获取失败的情况
                    }

                    uniqueHistories.put(key, historyMap);
                }
            }

            // 将去重后的记录转换为列表
            List<Map<String, Object>> processedHistories = new ArrayList<>(uniqueHistories.values());
            
            // 按观看时间倒序排序
            processedHistories.sort((a, b) -> {
                Date timeA = (Date) a.get("watchTime");
                Date timeB = (Date) b.get("watchTime");
                return timeB.compareTo(timeA);
            });

            response.put("code", 200);
            response.put("data", processedHistories);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取观看记录失败：" + e.getMessage());
        }
        return response;
    }
}