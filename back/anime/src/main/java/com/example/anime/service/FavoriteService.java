package com.example.anime.service;

import com.example.anime.model.Anime;
import com.example.anime.model.Favorite;
import com.example.anime.repository.AnimeRepository;
import com.example.anime.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {
    @Autowired
    private FavoriteRepository favoriteRepository;
    
    @Autowired
    private AnimeRepository animeRepository;
    
    // 添加收藏
    public Favorite addFavorite(Long userId, Long animeId) {
        // 检查是否已经收藏
        Optional<Favorite> existingFavorite = favoriteRepository.findByUserIdAndAnimeId(userId, animeId);
        if (existingFavorite.isPresent()) {
            return existingFavorite.get();
        }
        
        // 创建新收藏
        Favorite favorite = new Favorite(userId, animeId);
        return favoriteRepository.save(favorite);
    }
    
    // 取消收藏
    @javax.transaction.Transactional
    public void removeFavorite(Long userId, Long animeId) {
        favoriteRepository.deleteByUserIdAndAnimeId(userId, animeId);
    }
    
    // 获取用户的收藏列表
    public List<Anime> getUserFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
        List<Anime> favoriteAnimes = new ArrayList<>();
        
        for (Favorite favorite : favorites) {
            Anime anime = animeRepository.findByIdAndDeletedFalse(favorite.getAnimeId());
            if (anime != null) {
                favoriteAnimes.add(anime);
            }
        }
        
        return favoriteAnimes;
    }
    
    // 检查是否已收藏
    public boolean isFavorite(Long userId, Long animeId) {
        Optional<Favorite> favorite = favoriteRepository.findByUserIdAndAnimeId(userId, animeId);
        return favorite.isPresent();
    }
    
    // 获取用户的收藏实体列表
    public List<Favorite> getUserFavoriteEntities(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }
    
    // 获取所有收藏
    public List<Favorite> getAllFavorites() {
        return favoriteRepository.findAll();
    }

    // 根据用户ID获取收藏列表（用于管理员）
    public List<Favorite> getByUserId(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    // 根据ID删除收藏
    public void deleteById(Long id) {
        favoriteRepository.deleteById(id);
    }
    
    // 根据动漫ID删除所有收藏
    public void deleteByAnimeId(Long animeId) {
        favoriteRepository.deleteByAnimeId(animeId);
    }
    
    // 根据用户ID删除所有收藏
    public void deleteByUserId(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
        for (Favorite favorite : favorites) {
            favoriteRepository.delete(favorite);
        }
    }
}