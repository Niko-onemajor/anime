package com.example.anime.repository;

import com.example.anime.model.AnimeComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnimeCommentRepository extends JpaRepository<AnimeComment, Long> {
    // 根据动漫ID查询所有评论，按创建时间降序排序
    List<AnimeComment> findByAnimeIdOrderByCreateTimeDesc(Long animeId);
    
    // 根据动漫ID和父评论ID查询评论，按创建时间升序排序
    List<AnimeComment> findByAnimeIdAndParentIdOrderByCreateTimeAsc(Long animeId, Long parentId);
    
    // 根据作者ID查询评论，按创建时间降序排序
    List<AnimeComment> findByAuthorIdOrderByCreateTimeDesc(Long authorId);
    
    // 根据动漫ID删除所有评论（使用原生SQL）
    @Modifying
    @Query(value = "DELETE FROM anime_comments WHERE anime_id = :animeId", nativeQuery = true)
    void deleteByAnimeId(@Param("animeId") Long animeId);
    
    // 根据作者ID删除所有评论（使用原生SQL）
    @Modifying
    @Query(value = "DELETE FROM anime_comments WHERE author_id = :authorId", nativeQuery = true)
    void deleteCommentsByAuthorId(@Param("authorId") Long authorId);
    
    // 将所有引用特定评论的评论的parent_id设置为null（使用原生SQL）
    @Modifying
    @Query(value = "UPDATE anime_comments SET parent_id = NULL WHERE parent_id = :parentId", nativeQuery = true)
    void updateParentIdToNullByParentId(@Param("parentId") Long parentId);
}