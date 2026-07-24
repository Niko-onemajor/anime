package com.example.anime.controller;

import com.example.anime.model.Comment;
import com.example.anime.model.User;
import com.example.anime.repository.ForumCommentInteractionRepository;
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

    // 获取帖子的评论列表
    @PostMapping("/getComments")
    public Map<String, Object> getComments(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        Long postId = request.get("postId");

        List<Comment> comments = commentService.getCommentsByPostId(postId);

        // 构建带用户信息的评论列表
        List<Map<String, Object>> commentList = new ArrayList<>();
        for (Comment comment : comments) {
            // 重新计算点赞数和点踩数
            int likeCount = forumCommentInteractionRepository.countByCommentIdAndInteractionType(comment.getId(), 1);
            int dislikeCount = forumCommentInteractionRepository.countByCommentIdAndInteractionType(comment.getId(), 2);
            comment.setLikeCount(likeCount);
            comment.setDislikeCount(dislikeCount);
            
            Map<String, Object> commentMap = buildCommentMap(comment);
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

    // 添加评论
    @PostMapping("/addComment")
    public Map<String, Object> addComment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        Long postId = Long.valueOf(request.get("postId").toString());
        String username = (String) request.get("username");
        String content = (String) request.get("content");

        // 获取用户ID
        User user = userService.findByUsername(username);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }

        Comment comment = commentService.addComment(postId, user.getId(), content);

        // 构建返回数据
        Map<String, Object> commentMap = new HashMap<>();
        commentMap.put("id", comment.getId());
        commentMap.put("content", comment.getContent());
        commentMap.put("createTime", comment.getCreateTime());
        commentMap.put("likeCount", comment.getLikeCount());
        commentMap.put("dislikeCount", comment.getDislikeCount());
        commentMap.put("authorName", user.getUsername());
        commentMap.put("authorAvatar", user.getAvatar());

        response.put("code", 200);
        response.put("msg", "评论添加成功");
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
}
