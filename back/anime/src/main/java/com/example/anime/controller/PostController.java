package com.example.anime.controller;

import com.example.anime.model.Post;
import com.example.anime.model.User;
import com.example.anime.service.PostService;
import com.example.anime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/post")
public class PostController {
    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;

    // 获取所有帖子
    @GetMapping("/list")
    public List<Post> getPosts() {
        return postService.getAllPosts();
    }

    // 根据ID获取帖子
    @GetMapping("/detail/{id}")
    public Post getPost(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    // 根据作者ID获取帖子
    @GetMapping("/author/{authorId}")
    public List<Post> getPostsByAuthorId(@PathVariable Long authorId) {
        return postService.getPostsByAuthorId(authorId);
    }

    // 按时间排序获取帖子
    @GetMapping("/sort/time")
    public List<Post> getPostsByTime() {
        return postService.getPostsByTime();
    }

    // 按点赞数排序获取帖子
    @GetMapping("/sort/likes")
    public List<Post> getPostsByLikes() {
        return postService.getPostsByLikes();
    }

    // 按点踩数排序获取帖子
    @GetMapping("/sort/dislikes")
    public List<Post> getPostsByDislikes() {
        return postService.getPostsByDislikes();
    }

    // 发布帖子
    @PostMapping("/create")
    public Post createPost(@RequestBody Map<String, Object> request) {
        String title = (String) request.get("title");
        String content = (String) request.get("content");
        String username = (String) request.get("username");
        
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        
        return postService.savePost(post, username);
    }

    // 更新帖子
    @PostMapping("/update")
    public Post updatePost(@RequestBody Post post) {
        return postService.updatePost(post);
    }

    // 删除帖子
    @DeleteMapping("/delete/{id}")
    public void deletePost(@PathVariable Long id) {
        postService.deletePost(id);
    }

    // 点赞帖子
    @PostMapping("/like")
    public Map<String, Object> likePost(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        Long postId = Long.valueOf(request.get("postId").toString());
        String username = (String) request.get("username");

        // 获取用户ID
        User user = userService.findByUsername(username);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }

        Post post = postService.likePost(postId, user.getId());

        if (post == null) {
            response.put("code", 400);
            response.put("msg", "帖子不存在");
        } else {
            response.put("code", 200);
            response.put("msg", "操作成功");
            response.put("data", post);
        }
        return response;
    }

    // 点踩帖子
    @PostMapping("/dislike")
    public Map<String, Object> dislikePost(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        Long postId = Long.valueOf(request.get("postId").toString());
        String username = (String) request.get("username");

        // 获取用户ID
        User user = userService.findByUsername(username);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }

        Post post = postService.dislikePost(postId, user.getId());

        if (post == null) {
            response.put("code", 400);
            response.put("msg", "帖子不存在");
        } else {
            response.put("code", 200);
            response.put("msg", "操作成功");
            response.put("data", post);
        }
        return response;
    }

    // 搜索帖子
    @PostMapping("/search")
    public Map<String, Object> searchPosts(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String keyword = request.get("keyword");
            List<Post> posts = postService.findByTitleContaining(keyword);
            response.put("code", 200);
            response.put("data", posts);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "搜索帖子失败");
        }
        return response;
    }

    // 获取用户对帖子的互动状态
    @PostMapping("/interaction-status")
    public Map<String, Object> getUserPostInteractionStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long postId = Long.valueOf(request.get("postId").toString());
            String username = (String) request.get("username");

            // 获取用户ID
            User user = userService.findByUsername(username);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }

            Integer interactionType = postService.getUserPostInteraction(postId, user.getId());
            response.put("code", 200);
            response.put("data", interactionType);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取互动状态失败");
        }
        return response;
    }
}