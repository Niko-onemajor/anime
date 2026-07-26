package com.example.anime.repository;

import com.example.anime.model.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 根据作者ID查询帖子
    List<Post> findByAuthorId(Long authorId);
    
    // 按时间排序，加载作者信息
    @EntityGraph(attributePaths = {"author"})
    List<Post> findAllByOrderByCreateTimeDesc();
    
    // 按点赞数排序，加载作者信息
    @EntityGraph(attributePaths = {"author"})
    List<Post> findAllByOrderByLikeCountDesc();
    
    // 按点踩数排序，加载作者信息
    @EntityGraph(attributePaths = {"author"})
    List<Post> findAllByOrderByDislikeCountDesc();
    
    // 根据标题搜索帖子，加载作者信息
    @EntityGraph(attributePaths = {"author"})
    List<Post> findByTitleContaining(String keyword);
    
    // 根据标题或内容搜索帖子，加载作者信息
    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    List<Post> searchByKeyword(@Param("keyword") String keyword);
    
    // 查找所有帖子，加载作者信息
    @EntityGraph(attributePaths = {"author"})
    List<Post> findAll();
}