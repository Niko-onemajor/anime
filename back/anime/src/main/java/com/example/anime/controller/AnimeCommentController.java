package com.example.anime.controller;

import com.example.anime.model.AnimeComment;
import com.example.anime.repository.CommentInteractionRepository;
import com.example.anime.service.AnimeCommentService;
import com.example.anime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/anime/comment")
public class AnimeCommentController {
    @Autowired
    private AnimeCommentService animeCommentService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private CommentInteractionRepository commentInteractionRepository;

    // 添加评论
    @PostMapping("/add")
    public Map<String, Object> addComment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = Long.parseLong(request.get("animeId").toString());
            String username = (String) request.get("username");
            String content = (String) request.get("content");
            Long parentId = request.get("parentId") != null ? Long.parseLong(request.get("parentId").toString()) : null;

            // 获取用户ID
            Long authorId = userService.findByUsername(username).getId();

            // 保存评论
            AnimeComment savedComment = animeCommentService.saveComment(animeId, authorId, content, parentId);

            response.put("code", 200);
            response.put("msg", "评论成功");
            response.put("data", savedComment);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "评论失败：" + e.getMessage());
        }
        return response;
    }

    // 获取动漫的评论列表
    @PostMapping("/list")
    public Map<String, Object> getComments(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = Long.parseLong(request.get("animeId").toString());

            // 获取评论列表
            List<AnimeComment> comments = animeCommentService.getAnimeComments(animeId);
            
            // 处理评论数据，添加作者信息
            List<Map<String, Object>> processedComments = new ArrayList<>();
            for (AnimeComment comment : comments) {
                // 重新计算点赞数和点踩数
                int likeCount = commentInteractionRepository.countByCommentIdAndInteractionType(comment.getId(), 1);
                int dislikeCount = commentInteractionRepository.countByCommentIdAndInteractionType(comment.getId(), 2);
                comment.setLikeCount(likeCount);
                comment.setDislikeCount(dislikeCount);
                
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("id", comment.getId());
                commentMap.put("animeId", comment.getAnimeId());
                commentMap.put("authorId", comment.getAuthorId());
                commentMap.put("content", comment.getContent());
                commentMap.put("createTime", comment.getCreateTime());
                commentMap.put("parentId", comment.getParentId());
                commentMap.put("likeCount", likeCount);
                commentMap.put("dislikeCount", dislikeCount);
                
                // 添加作者信息
                try {
                    com.example.anime.model.User author = userService.findById(comment.getAuthorId());
                    if (author != null) {
                        Map<String, Object> authorMap = new HashMap<>();
                        authorMap.put("id", author.getId());
                        authorMap.put("username", author.getUsername());
                        authorMap.put("avatar", author.getAvatar());
                        commentMap.put("author", authorMap);
                    }
                } catch (Exception e) {
                    // 忽略作者信息获取失败的情况
                }
                
                processedComments.add(commentMap);
            }

            response.put("code", 200);
            response.put("data", processedComments);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取评论失败：" + e.getMessage());
        }
        return response;
    }

    // 获取评论的回复
    @PostMapping("/replies")
    public Map<String, Object> getReplies(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = Long.parseLong(request.get("animeId").toString());
            Long parentId = Long.parseLong(request.get("parentId").toString());

            // 获取回复列表
            List<AnimeComment> replies = animeCommentService.getCommentReplies(animeId, parentId);
            
            // 处理回复数据，添加作者信息
            List<Map<String, Object>> processedReplies = new ArrayList<>();
            for (AnimeComment reply : replies) {
                // 重新计算点赞数和点踩数
                int likeCount = commentInteractionRepository.countByCommentIdAndInteractionType(reply.getId(), 1);
                int dislikeCount = commentInteractionRepository.countByCommentIdAndInteractionType(reply.getId(), 2);
                reply.setLikeCount(likeCount);
                reply.setDislikeCount(dislikeCount);
                
                Map<String, Object> replyMap = new HashMap<>();
                replyMap.put("id", reply.getId());
                replyMap.put("animeId", reply.getAnimeId());
                replyMap.put("authorId", reply.getAuthorId());
                replyMap.put("content", reply.getContent());
                replyMap.put("createTime", reply.getCreateTime());
                replyMap.put("parentId", reply.getParentId());
                replyMap.put("likeCount", likeCount);
                replyMap.put("dislikeCount", dislikeCount);
                
                // 添加作者信息
                try {
                    com.example.anime.model.User author = userService.findById(reply.getAuthorId());
                    if (author != null) {
                        Map<String, Object> authorMap = new HashMap<>();
                        authorMap.put("id", author.getId());
                        authorMap.put("username", author.getUsername());
                        authorMap.put("avatar", author.getAvatar());
                        replyMap.put("author", authorMap);
                    }
                } catch (Exception e) {
                    // 忽略作者信息获取失败的情况
                }
                
                processedReplies.add(replyMap);
            }

            response.put("code", 200);
            response.put("data", processedReplies);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取回复失败：" + e.getMessage());
        }
        return response;
    }

    // 点赞评论
    @PostMapping("/like")
    public Map<String, Object> likeComment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long commentId = Long.parseLong(request.get("commentId").toString());
            String username = (String) request.get("username");
            
            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();

            // 点赞评论
            AnimeComment commented = animeCommentService.likeComment(commentId, userId);
            
            if (commented == null) {
                response.put("code", 400);
                response.put("msg", "您已经点赞过该评论");
            } else {
                response.put("code", 200);
                response.put("msg", "点赞成功");
                response.put("data", commented);
            }
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "点赞失败：" + e.getMessage());
        }
        return response;
    }

    // 点踩评论
    @PostMapping("/dislike")
    public Map<String, Object> dislikeComment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long commentId = Long.parseLong(request.get("commentId").toString());
            String username = (String) request.get("username");
            
            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();

            // 点踩评论
            AnimeComment commented = animeCommentService.dislikeComment(commentId, userId);
            
            if (commented == null) {
                response.put("code", 400);
                response.put("msg", "您已经点踩过该评论");
            } else {
                response.put("code", 200);
                response.put("msg", "点踩成功");
                response.put("data", commented);
            }
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "点踩失败：" + e.getMessage());
        }
        return response;
    }

    // 删除评论
    @PostMapping("/delete")
    public Map<String, Object> deleteComment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long commentId = Long.parseLong(request.get("commentId").toString());

            // 删除评论
            animeCommentService.deleteComment(commentId);

            response.put("code", 200);
            response.put("msg", "删除成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "删除失败：" + e.getMessage());
        }
        return response;
    }
}