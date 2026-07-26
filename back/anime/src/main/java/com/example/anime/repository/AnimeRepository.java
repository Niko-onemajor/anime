package com.example.anime.repository;

import com.example.anime.model.Anime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AnimeRepository extends JpaRepository<Anime, Long> {
    // 根据年份查询
    List<Anime> findByYear(String year);
    
    // 根据首字母查询
    List<Anime> findByLetter(String letter);
    
    // 根据年份查询未删除的动漫
    List<Anime> findByYearAndDeletedFalse(String year);
    
    // 根据首字母查询未删除的动漫
    List<Anime> findByLetterAndDeletedFalse(String letter);
    
    // 根据关键字搜索
    @Query("SELECT a FROM Anime a WHERE a.title LIKE %:keyword% AND a.deleted = false")
    List<Anime> searchByKeyword(@Param("keyword") String keyword);
    
    // 按评分排序
    List<Anime> findByDeletedFalseOrderByRatingDesc();
    
    // 按年份排序
    List<Anime> findByDeletedFalseOrderByYearDesc();
    
    // 根据状态查询
    List<Anime> findByStatusAndDeletedFalse(int status);
    
    // 根据状态分页查询（上架且未删除）
    Page<Anime> findByStatusAndDeletedFalse(int status, Pageable pageable);
    
    // 查询所有未删除的动漫
    List<Anime> findByDeletedFalse();
    
    // 查询所有已删除的动漫
    List<Anime> findByDeletedTrue();
    
    // 根据ID查询未删除的动漫
    Anime findByIdAndDeletedFalse(Long id);
}