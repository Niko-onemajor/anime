package com.example.anime.controller;

import com.example.anime.model.Comment;
import com.example.anime.model.User;
import com.example.anime.model.Post;
import com.example.anime.repository.ForumCommentInteractionRepository;
import com.example.anime.repository.PostRepository;
import com.example.anime.service.CommentService;
import com.example.anime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private ForumCommentInteractionRepository forumCommentInteractionRepository;
    
    @Autowired
    private PostRepository postRepository;

    // 获取帖子的评论列表（嵌套结构：顶级评论 + 回复）
    @PostMapping("/getComments")
    public Map<String, Object> getComments(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        Long postId = request.get("postId");

        // 获取顶级评论（parentId为null的评论）
        List<Comment> topLevelComments = commentService.getTopLevelComments(postId);

        // 构建带用户信息和嵌套回复的评论列表
        List<Map<String, Object>> commentList = new ArrayList<>();
        for (Comment comment : topLevelComments) {
            // 重新计算点赞数和点踩数
            int likeCount = forumCommentInteractionRepository.countByCommentIdAndInteractionType(comment.getId(), 1);
            int dislikeCount = forumCommentInteractionRepository.countByCommentIdAndInteractionType(comment.getId(), 2);
            comment.setLikeCount(likeCount);
            comment.setDislikeCount(dislikeCount);
            
            Map<String, Object> commentMap = buildCommentMap(comment);
            
            // 获取该评论的回复
            List<Comment> replies = commentService.getReplies(comment.getId());
            List<Map<String, Object>> replyList = new ArrayList<>();
            for (Comment reply : replies) {
                int replyLikeCount = forumCommentInteractionRepository.countByCommentIdAndInteractionType(reply.getId(), 1);
                int replyDislikeCount = forumCommentInteractionRepository.countByCommentIdAndInteractionType(reply.getId(), 2);
                reply.setLikeCount(replyLikeCount);
                reply.setDislikeCount(replyDislikeCount);
                
                Map<String, Object> replyMap = buildCommentMap(reply);
                // 添加被回复人用户名
                User parentAuthor = userService.findById(comment.getAuthorId());
                replyMap.put("replyToName", parentAuthor != null ? parentAuthor.getUsername() : "");
                replyList.add(replyMap);
            }
            commentMap.put("replies", replyList);
            
            commentList.add(commentMap);
        }

        response.put("code", 200);
        response.put("msg", "获取评论成功");
        response.put("data", commentList);
        return response;
    }
    
    // 构建评论信息
    private Map<String, Object> buildCommentMap(Comment comment) {
        Map<String, Object> commentMap = new HashMap<>();
        commentMap.put("id", comment.getId());
        commentMap.put("content", comment.getContent());
        commentMap.put("createTime", comment.getCreateTime());
        commentMap.put("likeCount", comment.getLikeCount());
        commentMap.put("dislikeCount", comment.getDislikeCount());
        
        // 获取作者信息
        User user = userService.findById(comment.getAuthorId());
        if (user != null) {
            commentMap.put("authorName", user.getUsername());
            commentMap.put("authorAvatar", user.getAvatar());
        }

        return commentMap;
    }

    // 添加评论（支持回复）
    @PostMapping("/addComment")
    public Map<String, Object> addComment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        Long postId = Long.valueOf(request.get("postId").toString());
        String username = (String) request.get("username");
        String content = (String) request.get("content");
        
        // parentId 可选，用于回复
        Long parentId = null;
        if (request.get("parentId") != null) {
            parentId = Long.valueOf(request.get("parentId").toString());
        }

        // 获取用户ID
        User user = userService.findByUsername(username);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }

        Comment comment = commentService.addComment(postId, user.getId(), content, parentId);

        // 支持测试标记
        if (request.get("isTest") != null) {
            comment.setIsTest(Boolean.valueOf(request.get("isTest").toString()));
            comment = commentService.save(comment);
        }

        // 构建返回数据
        Map<String, Object> commentMap = new HashMap<>();
        commentMap.put("id", comment.getId());
        commentMap.put("content", comment.getContent());
        commentMap.put("createTime", comment.getCreateTime());
        commentMap.put("likeCount", comment.getLikeCount());
        commentMap.put("dislikeCount", comment.getDislikeCount());
        commentMap.put("parentId", comment.getParentId());
        commentMap.put("authorName", user.getUsername());
        commentMap.put("authorAvatar", user.getAvatar());
        
        // 如果是回复，添加被回复人用户名
        if (parentId != null) {
            Comment parentComment = commentService.findById(parentId);
            if (parentComment != null) {
                User parentAuthor = userService.findById(parentComment.getAuthorId());
                commentMap.put("replyToName", parentAuthor != null ? parentAuthor.getUsername() : "");
            }
        }

        response.put("code", 200);
        response.put("msg", parentId != null ? "回复添加成功" : "评论添加成功");
        response.put("data", commentMap);
        return response;
    }

    // 删除评论
    @PostMapping("/deleteComment")
    public Map<String, Object> deleteComment(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        Long commentId = request.get("commentId");

        commentService.deleteComment(commentId);

        response.put("code", 200);
        response.put("msg", "评论删除成功");
        return response;
    }



    // 点赞评论
    @PostMapping("/like")
    public Map<String, Object> likeComment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        Long commentId = Long.valueOf(request.get("commentId").toString());
        String username = (String) request.get("username");

        // 获取用户ID
        User user = userService.findByUsername(username);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }

        Comment comment = commentService.likeComment(commentId, user.getId());

        if (comment == null) {
            response.put("code", 400);
            response.put("msg", "您已经点赞过该评论");
        } else {
            response.put("code", 200);
            response.put("msg", "点赞成功");
            response.put("data", comment);
        }
        return response;
    }

    // 点踩评论
    @PostMapping("/dislike")
    public Map<String, Object> dislikeComment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        Long commentId = Long.valueOf(request.get("commentId").toString());
        String username = (String) request.get("username");

        // 获取用户ID
        User user = userService.findByUsername(username);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }

        Comment comment = commentService.dislikeComment(commentId, user.getId());

        if (comment == null) {
            response.put("code", 400);
            response.put("msg", "您已经点踩过该评论");
        } else {
            response.put("code", 200);
            response.put("msg", "点踩成功");
            response.put("data", comment);
        }
        return response;
    }

    // 获取用户的所有论坛评论（带帖子信息）
    @GetMapping("/author/{authorId}")
    public Map<String, Object> getCommentsByAuthor(@PathVariable Long authorId) {
        Map<String, Object> response = new HashMap<>();
        List<Comment> comments = commentService.getCommentsByAuthorId(authorId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Comment comment : comments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", comment.getId());
            item.put("content", comment.getContent());
            item.put("createTime", comment.getCreateTime());
            item.put("likeCount", comment.getLikeCount());
            item.put("dislikeCount", comment.getDislikeCount());
            item.put("type", "forum");
            // 获取帖子信息
            Post post = postRepository.findById(comment.getPostId()).orElse(null);
            if (post != null) {
                item.put("targetId", post.getId());
                item.put("targetTitle", post.getTitle());
            }
            result.add(item);
        }
        response.put("code", 200);
        response.put("data", result);
        return response;
    }
}
