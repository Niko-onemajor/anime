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
    
    // 根据关键字搜索（非删除、非测试，用户端使用）
    @Query("SELECT a FROM Anime a WHERE a.title LIKE %:keyword% AND (a.deleted IS NULL OR a.deleted = false) AND (a.isTest IS NULL OR a.isTest = false)")
    List<Anime> searchByKeyword(@Param("keyword") String keyword);
    
    // 根据关键字搜索（非删除，管理员端使用，不过滤测试数据）
    @Query("SELECT a FROM Anime a WHERE a.title LIKE %:keyword% AND (a.deleted IS NULL OR a.deleted = false)")
    List<Anime> searchByKeywordAdmin(@Param("keyword") String keyword);
    
    // 按评分排序（非删除、非测试）
    List<Anime> findByDeletedFalseAndIsTestFalseOrderByRatingDesc();
    
    // 按年份排序（非删除、非测试）
    List<Anime> findByDeletedFalseAndIsTestFalseOrderByYearDesc();
    
    // 根据状态查询（非删除、非测试）
    List<Anime> findByStatusAndDeletedFalseAndIsTestFalse(int status);
    
    // 根据状态分页查询（上架且未删除、非测试）
    Page<Anime> findByStatusAndDeletedFalseAndIsTestFalse(int status, Pageable pageable);
    
    // 查询所有未删除的非测试动漫（用户端）
    List<Anime> findByDeletedFalseAndIsTestFalse();
    
    // 查询所有未删除的动漫（管理员端，不过滤测试数据）
    List<Anime> findByDeletedFalse();
    
    // 查询所有已删除的动漫
    List<Anime> findByDeletedTrue();
    
    // 根据ID查询未删除的动漫
    Anime findByIdAndDeletedFalse(Long id);
    
    // 根据年份查询未删除的非测试动漫
    List<Anime> findByYearAndDeletedFalseAndIsTestFalse(String year);
    
    // 根据首字母查询未删除的非测试动漫
    List<Anime> findByLetterAndDeletedFalseAndIsTestFalse(String letter);
}