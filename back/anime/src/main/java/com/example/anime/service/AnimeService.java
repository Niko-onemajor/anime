package com.example.anime.service;

import com.example.anime.model.Anime;
import com.example.anime.model.Episode;
import com.example.anime.model.WatchHistory;
import com.example.anime.repository.AnimeRepository;
import com.example.anime.repository.WatchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnimeService {
    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;
    
    @Autowired
    private EpisodeService episodeService;
    
    @Autowired
    private WatchHistoryService watchHistoryService;
    
    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private AnimeRatingService animeRatingService;
    
    @Autowired
    private AnimeCommentService animeCommentService;

    // 获取所有动漫（只返回上架状态且非删除）
    public List<Anime> getAllAnimes() {
        return animeRepository.findByStatusAndDeletedFalse(1);
    }

    // 分页获取动漫（上架且非删除）
    public Page<Anime> getAllAnimesPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return animeRepository.findByStatusAndDeletedFalse(1, pageable);
    }

    // 根据ID获取动漫（非删除）
    public Anime getAnimeById(Long id) {
        return animeRepository.findByIdAndDeletedFalse(id);
    }

    // 根据年份获取动漫（非删除）
    public List<Anime> getAnimesByYear(String year) {
        return animeRepository.findByYearAndDeletedFalse(year);
    }

    // 根据首字母获取动漫（非删除）
    public List<Anime> getAnimesByLetter(String letter) {
        return animeRepository.findByLetterAndDeletedFalse(letter);
    }

    // 根据关键字搜索动漫（非删除）
    public List<Anime> searchAnimes(String keyword) {
        return animeRepository.searchByKeyword(keyword);
    }

    // 按评分排序获取动漫（非删除）
    public List<Anime> getAnimesByRating() {
        return animeRepository.findByDeletedFalseOrderByRatingDesc();
    }

    // 按观看次数排序获取热门动漫（非删除）
    public List<Map<String, Object>> getPopularAnimesByWatchCount() {
        return getRankingByTimeRange(null);
    }
    
    // 获取周榜
    public List<Map<String, Object>> getWeeklyRanking() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        Date oneWeekAgo = calendar.getTime();
        return getRankingByTimeRange(oneWeekAgo);
    }
    
    // 获取月榜
    public List<Map<String, Object>> getMonthlyRanking() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Date oneMonthAgo = calendar.getTime();
        return getRankingByTimeRange(oneMonthAgo);
    }
    
    // 获取年榜
    public List<Map<String, Object>> getYearlyRanking() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        Date oneYearAgo = calendar.getTime();
        return getRankingByTimeRange(oneYearAgo);
    }
    
    // 按时间范围获取排行榜
    private List<Map<String, Object>> getRankingByTimeRange(Date startTime) {
        // 获取观看历史
        List<WatchHistory> watchHistories;
        if (startTime != null) {
            // 获取指定时间范围内的观看历史
            watchHistories = watchHistoryRepository.findAll().stream()
                    .filter(history -> history.getWatchTime().after(startTime))
                    .collect(Collectors.toList());
        } else {
            // 获取所有观看历史
            watchHistories = watchHistoryRepository.findAll();
        }
        
        // 计算动漫观看次数
        Map<Long, Integer> animeWatchCount = new HashMap<>();
        for (WatchHistory history : watchHistories) {
            animeWatchCount.put(history.getAnimeId(), animeWatchCount.getOrDefault(history.getAnimeId(), 0) + 1);
        }

        // 按观看次数降序排序，观看次数相同时按评分降序排序
        List<Map.Entry<Long, Integer>> sortedEntries = animeWatchCount.entrySet().stream()
                // 过滤掉被删除的动漫
                .filter(entry -> {
                    Anime anime = animeRepository.findByIdAndDeletedFalse(entry.getKey());
                    return anime != null;
                })
                .sorted((entry1, entry2) -> {
                    // 首先按观看次数降序排序
                    int watchCountCompare = entry2.getValue().compareTo(entry1.getValue());
                    if (watchCountCompare != 0) {
                        return watchCountCompare;
                    }
                    // 观看次数相同时，按评分降序排序
                    Anime anime1 = animeRepository.findByIdAndDeletedFalse(entry1.getKey());
                    Anime anime2 = animeRepository.findByIdAndDeletedFalse(entry2.getKey());
                    
                    // 确保两个动漫都存在且未删除
                    if (anime1 == null && anime2 == null) {
                        return 0;
                    } else if (anime1 == null) {
                        return 1; // anime1为null，排在后面
                    } else if (anime2 == null) {
                        return -1; // anime2为null，排在后面
                    } else {
                        // 按评分降序排序
                        return Double.compare(anime2.getRating(), anime1.getRating());
                    }
                })
                .limit(5)
                .collect(Collectors.toList());

        // 获取热门动漫及其观看次数
        List<Map<String, Object>> popularAnimes = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : sortedEntries) {
            Optional<Anime> animeOptional = animeRepository.findById(entry.getKey());
            if (animeOptional.isPresent()) {
                Anime anime = animeOptional.get();
                if (!anime.getDeleted()) {
                    Map<String, Object> animeWithCount = new HashMap<>();
                    animeWithCount.put("id", anime.getId());
                    animeWithCount.put("title", anime.getTitle());
                    animeWithCount.put("image", anime.getImage());
                    animeWithCount.put("year", anime.getYear());
                    animeWithCount.put("genre", anime.getGenre());
                    animeWithCount.put("rating", anime.getRating());
                    animeWithCount.put("status", anime.getStatus());
                    animeWithCount.put("watchCount", entry.getValue());
                    popularAnimes.add(animeWithCount);
                }
            }
        }

        // 如果热门动漫不足5个，补充其他动漫
        if (popularAnimes.size() < 5) {
            List<Anime> allAnime = animeRepository.findByDeletedFalse();
            Set<Long> popularAnimeIds = sortedEntries.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            
            // 过滤出未在热门动漫中的动漫，并按评分降序排序
            List<Anime> remainingAnimes = allAnime.stream()
                    .filter(anime -> !popularAnimeIds.contains(anime.getId()))
                    .sorted((a1, a2) -> Double.compare(a2.getRating(), a1.getRating()))
                    .collect(Collectors.toList());
            
            for (Anime anime : remainingAnimes) {
                Map<String, Object> animeWithCount = new HashMap<>();
                animeWithCount.put("id", anime.getId());
                animeWithCount.put("title", anime.getTitle());
                animeWithCount.put("image", anime.getImage());
                animeWithCount.put("year", anime.getYear());
                animeWithCount.put("genre", anime.getGenre());
                animeWithCount.put("rating", anime.getRating());
                animeWithCount.put("status", anime.getStatus());
                animeWithCount.put("watchCount", 0);
                popularAnimes.add(animeWithCount);
                if (popularAnimes.size() >= 5) {
                    break;
                }
            }
        }

        return popularAnimes;
    }

    // 获取动漫的观看次数
    public int getAnimeWatchCount(Long animeId) {
        // 获取所有观看历史
        List<WatchHistory> allWatchHistories = watchHistoryRepository.findAll();
        
        // 计算指定动漫的观看次数
        int count = 0;
        for (WatchHistory history : allWatchHistories) {
            if (history.getAnimeId().equals(animeId)) {
                count++;
            }
        }
        
        return count;
    }

    // 按年份排序获取动漫（非删除）
    public List<Anime> getAnimesByYear() {
        return animeRepository.findByDeletedFalseOrderByYearDesc();
    }

    // 保存动漫
    public Anime saveAnime(Anime anime) {
        return animeRepository.save(anime);
    }

    // 删除动漫（逻辑删除）
    public void deleteAnime(Long id) {
        Anime anime = animeRepository.findByIdAndDeletedFalse(id);
        if (anime != null) {
            anime.setDeleted(true);
            anime.setDeletedAt(new java.util.Date());
            animeRepository.save(anime);
        }
    }

    // 获取所有动漫（用于管理员，非删除）
    public List<Anime> findAll() {
        return animeRepository.findByDeletedFalse();
    }

    // 根据标题搜索动漫（用于管理员，非删除）
    public List<Anime> findByTitleContaining(String keyword) {
        return animeRepository.searchByKeyword(keyword);
    }

    // 根据ID查找动漫（用于管理员，非删除）
    public Anime findById(Long id) {
        return animeRepository.findByIdAndDeletedFalse(id);
    }

    // 保存动漫（用于管理员）
    public Anime save(Anime anime) {
        return animeRepository.save(anime);
    }

    // 根据ID删除动漫（用于管理员，逻辑删除）
    public void deleteById(Long id) {
        Anime anime = animeRepository.findByIdAndDeletedFalse(id);
        if (anime != null) {
            anime.setDeleted(true);
            anime.setDeletedAt(new java.util.Date());
            animeRepository.save(anime);
        }
    }
    
    // 获取所有已删除的动漫
    public List<Anime> findAllDeleted() {
        return animeRepository.findByDeletedTrue();
    }
    
    // 恢复已删除的动漫
    public void restore(Long id) {
        Anime anime = animeRepository.findById(id).orElse(null);
        if (anime != null) {
            anime.setDeleted(false);
            anime.setDeletedAt(null);
            animeRepository.save(anime);
            
            // 不再自动恢复集数，集数需要手动恢复
            // 这样可以避免恢复一集时所有集数都被恢复的问题
        }
    }
    
    // 彻底删除动漫
    @javax.transaction.Transactional
    public void hardDelete(Long id) {
        try {
            System.out.println("开始彻底删除动漫，ID: " + id);
            
            // 1. 先删除所有相关的评论互动记录和评论
            System.out.println("删除动漫评论...");
            animeCommentService.deleteByAnimeId(id);
            
            // 2. 删除所有相关的观看记录
            System.out.println("删除观看记录...");
            watchHistoryService.deleteByAnimeId(id);
            
            // 3. 删除所有相关的收藏
            System.out.println("删除收藏...");
            favoriteService.deleteByAnimeId(id);
            
            // 4. 删除所有相关的评分
            System.out.println("删除评分...");
            animeRatingService.deleteByAnimeId(id);
            
            // 5. 删除所有相关的集数（包括已删除的）
            System.out.println("删除集数...");
            List<Episode> episodes = episodeService.getEpisodesByAnimeId(id);
            System.out.println("找到 " + episodes.size() + " 个未删除的集数");
            for (Episode episode : episodes) {
                System.out.println("删除集数 ID: " + episode.getId());
                episodeService.hardDelete(episode.getId());
            }
            
            List<Episode> deletedEpisodes = episodeService.findAllDeleted();
            int deletedCount = 0;
            for (Episode episode : deletedEpisodes) {
                if (episode.getAnimeId().equals(id)) {
                    deletedCount++;
                    System.out.println("删除已删除的集数 ID: " + episode.getId());
                    episodeService.hardDelete(episode.getId());
                }
            }
            System.out.println("找到并删除 " + deletedCount + " 个已删除的集数");
            
            // 6. 最后删除动漫本身
            System.out.println("删除动漫本身...");
            animeRepository.deleteById(id);
            System.out.println("动漫彻底删除成功，ID: " + id);
        } catch (javax.persistence.PersistenceException e) {
            // 处理数据库相关异常
            System.out.println("彻底删除动漫失败: 数据库操作错误 - " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，让调用方知道删除失败
        } catch (Exception e) {
            // 处理其他异常
            System.out.println("彻底删除动漫失败: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，让调用方知道删除失败
        }
    }
    
    // 根据ID查找动漫（包括已删除的）
    public Anime findByIdIncludingDeleted(Long id) {
        return animeRepository.findById(id).orElse(null);
    }
}