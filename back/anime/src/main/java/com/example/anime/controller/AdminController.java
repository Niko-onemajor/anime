package com.example.anime.controller;

import com.example.anime.model.User;
import com.example.anime.model.Anime;
import com.example.anime.model.Post;
import com.example.anime.model.Comment;
import com.example.anime.model.Episode;
import com.example.anime.service.UserService;
import com.example.anime.service.AnimeService;
import com.example.anime.service.PostService;
import com.example.anime.service.CommentService;
import com.example.anime.service.EpisodeService;
import com.example.anime.handler.CommentWebSocketHandler;
import com.example.anime.utils.OSSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;

    @Autowired
    private AnimeService animeService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private OSSUtil ossUtil;

    @Autowired
    private EpisodeService episodeService;
    
    @Autowired
    private CommentWebSocketHandler commentWebSocketHandler;

    // 用户管理 API
    @PostMapping("/users")
    public Map<String, Object> getUsers(@RequestBody Map<String, Integer> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            int page = request.getOrDefault("page", 1);
            int size = request.getOrDefault("size", 6); // 每页显示6个用户
            org.springframework.data.domain.Page<User> userPage = userService.findAll(page, size);
            List<User> users = userPage.getContent();
            // 显示实际密码或加密密码标识
            for (User user : users) {
                String password = user.getPassword();
                if (password != null) {
                    if (password.startsWith("$2a$")) {
                        // 加密密码，显示前几位作为标识
                        user.setPassword(password.substring(0, 10) + "...");
                    }
                    // 明文密码直接显示
                }
            }
            response.put("code", 200);
            response.put("data", users);
            response.put("total", userPage.getTotalElements());
            response.put("pages", userPage.getTotalPages());
            response.put("currentPage", page);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "获取用户列表失败");
        }
        return response;
    }

    @PostMapping("/users/search")
    public Map<String, Object> searchUsers(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String keyword = (String) request.get("keyword");
            Object pageObj = request.getOrDefault("page", 1);
            Object sizeObj = request.getOrDefault("size", 6); // 每页显示6个用户
            int page = pageObj instanceof Number ? ((Number) pageObj).intValue() : 1;
            int size = sizeObj instanceof Number ? ((Number) sizeObj).intValue() : 6;
            org.springframework.data.domain.Page<User> userPage = userService.findByUsernameContaining(keyword, page, size);
            List<User> users = userPage.getContent();
            // 显示实际密码或加密密码标识
            for (User user : users) {
                String password = user.getPassword();
                if (password != null) {
                    if (password.startsWith("$2a$")) {
                        // 加密密码，显示前几位作为标识
                        user.setPassword(password.substring(0, 10) + "...");
                    }
                    // 明文密码直接显示
                }
            }
            response.put("code", 200);
            response.put("data", users);
            response.put("total", userPage.getTotalElements());
            response.put("pages", userPage.getTotalPages());
            response.put("currentPage", page);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "搜索用户失败");
        }
        return response;
    }

    @PostMapping("/users/add")
    public Map<String, Object> addUser(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            User newUser = userService.register(user.getUsername(), user.getPassword());
            if (newUser != null) {
                // 更新其他字段
                newUser.setEmail(user.getEmail());
                newUser.setBirthday(user.getBirthday());
                newUser.setRole(user.getRole());
                newUser.setGender(user.getGender());
                newUser.setRegion(user.getRegion());
                newUser.setSignature(user.getSignature());
                newUser.setFavorite(user.getFavorite());
                newUser.setAvatar(user.getAvatar());
                userService.save(newUser);
                response.put("code", 200);
                response.put("msg", "用户添加成功");
            } else {
                response.put("code", 400);
                response.put("msg", "用户名已存在");
            }
        } catch (IllegalArgumentException e) {
            // 捕获参数异常，返回400错误
            response.put("code", 400);
            response.put("msg", e.getMessage());
        } catch (Exception e) {
            // 捕获其他异常，返回500错误
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "添加用户失败: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/users/update")
    public Map<String, Object> updateUser(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            User existingUser = userService.findById(user.getId());
            if (existingUser != null) {
                // 检查新用户名是否已被其他用户使用
                if (!existingUser.getUsername().equals(user.getUsername())) {
                    User checkUser = userService.findByUsername(user.getUsername());
                    if (checkUser != null && !checkUser.getId().equals(existingUser.getId())) {
                        response.put("code", 400);
                        response.put("msg", "用户名已存在，请换一个名字试试");
                        return response;
                    }
                }
                
                // 更新用户信息
                existingUser.setUsername(user.getUsername());
                existingUser.setEmail(user.getEmail());
                existingUser.setBirthday(user.getBirthday());
                existingUser.setRole(user.getRole());
                // 如果密码不为空，更新密码（使用加密密码）
                if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                    existingUser.setPassword(userService.encodePassword(user.getPassword()));
                }
                // 更新头像
                if (user.getAvatar() != null) {
                    existingUser.setAvatar(user.getAvatar());
                }
                // 更新性别、地区、签名和喜爱的动漫
                existingUser.setGender(user.getGender());
                existingUser.setRegion(user.getRegion());
                existingUser.setSignature(user.getSignature());
                existingUser.setFavorite(user.getFavorite());
                userService.save(existingUser);
                response.put("code", 200);
                response.put("msg", "用户更新成功");
            } else {
                response.put("code", 400);
                response.put("msg", "用户不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "更新用户失败: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/users/delete")
    public Map<String, Object> deleteUser(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = request.get("id");
            System.out.println("开始删除用户，ID: " + userId);
            
            // 1. 先检查用户是否存在
            System.out.println("检查用户是否存在...");
            User user = userService.findById(userId);
            if (user == null) {
                System.out.println("用户不存在，ID: " + userId);
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }
            System.out.println("用户存在，ID: " + userId + ", 用户名: " + user.getUsername());
            
            // 2. 调用UserService的deleteUser方法删除用户及其相关数据
            System.out.println("调用UserService.deleteUser删除用户...");
            userService.deleteUser(userId);
            System.out.println("用户删除成功，ID: " + userId);
            
            response.put("code", 200);
            response.put("msg", "用户删除成功");
        } catch (javax.persistence.PersistenceException e) {
            // 处理数据库相关异常
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "删除用户失败: 数据库操作错误 - " + e.getMessage());
        } catch (Exception e) {
            // 处理其他异常
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "删除用户失败: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/users/update-password")
    public Map<String, Object> updatePassword(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = Long.valueOf(request.get("id").toString());
            String oldPassword = (String) request.get("oldPassword");
            String newPassword = (String) request.get("newPassword");
            
            // 检查用户是否存在
            User user = userService.findById(userId);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }
            
            // 验证原密码
            if (!userService.validatePassword(oldPassword, user.getPassword())) {
                response.put("code", 400);
                response.put("msg", "原密码错误");
                return response;
            }
            
            // 验证新密码格式
            if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
                response.put("code", 400);
                response.put("msg", "密码长度必须在6-20个字符之间");
                return response;
            }
            
            // 更新密码
            user.setPassword(userService.encodePassword(newPassword));
            userService.save(user);
            
            response.put("code", 200);
            response.put("msg", "密码修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "修改密码失败: " + e.getMessage());
        }
        return response;
    }
    
    @PostMapping("/users/reset-password")
    public Map<String, Object> resetPassword(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = request.get("id");
            
            // 检查用户是否存在
            User user = userService.findById(userId);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }
            
            // 重置密码为123456
            String defaultPassword = "123456";
            user.setPassword(userService.encodePassword(defaultPassword));
            userService.save(user);
            
            response.put("code", 200);
            response.put("msg", "密码重置成功");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "重置密码失败: " + e.getMessage());
        }
        return response;
    }

    // 动漫管理 API
    @PostMapping("/animes")
    public Map<String, Object> getAnimes() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Anime> animes = animeService.findAll();
            response.put("code", 200);
            response.put("data", animes);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取动漫列表失败");
        }
        return response;
    }

    @PostMapping("/animes/search")
    public Map<String, Object> searchAnimes(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String keyword = request.get("keyword");
            List<Anime> animes = animeService.findByTitleContaining(keyword);
            response.put("code", 200);
            response.put("data", animes);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "搜索动漫失败");
        }
        return response;
    }

    @PostMapping("/animes/add")
    public Map<String, Object> addAnime(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 创建动漫对象
            Anime anime = new Anime();
            anime.setTitle((String) request.get("title"));
            anime.setDescription((String) request.get("description"));
            anime.setImage((String) request.get("image"));
            String yearStr = (String) request.get("year");
            if (yearStr != null && !yearStr.isEmpty()) {
                try {
                    anime.setYear(Integer.parseInt(yearStr));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 评分由用户评分自动计算，不允许手动设置
            anime.setRating(0.0);
            anime.setLetter((String) request.get("letter"));
            anime.setGenre((String) request.get("genre"));
            anime.setStatus((Integer) request.get("status"));
            
            // 保存动漫
            Anime savedAnime = animeService.save(anime);
            
            // 处理集数
            Object episodesObj = request.get("episodes");
            if (episodesObj instanceof List) {
                List<?> episodesList = (List<?>) episodesObj;
                for (Object episodeObj : episodesList) {
                    if (episodeObj instanceof Map) {
                        Map<?, ?> episodeMap = (Map<?, ?>) episodeObj;
                        Episode episode = new Episode();
                        episode.setAnimeId(savedAnime.getId());
                        Object episodeNumberObj = episodeMap.get("episodeNumber");
                        if (episodeNumberObj instanceof Integer) {
                            episode.setEpisodeNumber((Integer) episodeNumberObj);
                        }
                        Object videoUrlObj = episodeMap.get("videoUrl");
                        if (videoUrlObj instanceof String) {
                            episode.setVideoUrl((String) videoUrlObj);
                        }
                        episodeService.save(episode);
                    }
                }
            }
            
            response.put("code", 200);
            response.put("msg", "动漫添加成功");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "添加动漫失败: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/animes/update")
    public Map<String, Object> updateAnime(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = Long.parseLong(request.get("id").toString());
            Anime existingAnime = animeService.findById(animeId);
            
            if (existingAnime != null) {
                // 更新动漫信息
                // 处理title字段，支持字符串和整数类型
                Object titleObj = request.get("title");
                if (titleObj instanceof String) {
                    existingAnime.setTitle((String) titleObj);
                } else if (titleObj instanceof Integer) {
                    existingAnime.setTitle(String.valueOf(titleObj));
                }
                // 处理description字段，支持字符串和整数类型
                Object descriptionObj = request.get("description");
                if (descriptionObj instanceof String) {
                    existingAnime.setDescription((String) descriptionObj);
                } else if (descriptionObj instanceof Integer) {
                    existingAnime.setDescription(String.valueOf(descriptionObj));
                }
                // 处理image字段，支持字符串和整数类型
                Object imageObj = request.get("image");
                if (imageObj instanceof String) {
                    existingAnime.setImage((String) imageObj);
                } else if (imageObj instanceof Integer) {
                    existingAnime.setImage(String.valueOf(imageObj));
                }
                // 处理year字段，支持字符串和整数类型
                Object yearObj = request.get("year");
                if (yearObj instanceof String) {
                    String yearStr = (String) yearObj;
                    if (yearStr != null && !yearStr.isEmpty()) {
                        try {
                            existingAnime.setYear(Integer.parseInt(yearStr));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (yearObj instanceof Integer) {
                    existingAnime.setYear((Integer) yearObj);
                }
                // 评分由用户评分自动计算，不允许手动设置
                // 保持原有的评分值，不进行修改
                // existingAnime.setRating(...);
                // 处理letter字段，支持字符串和整数类型
                Object letterObj = request.get("letter");
                if (letterObj instanceof String) {
                    existingAnime.setLetter((String) letterObj);
                } else if (letterObj instanceof Integer) {
                    existingAnime.setLetter(String.valueOf(letterObj));
                }
                // 处理genre字段，支持字符串和整数类型
                Object genreObj = request.get("genre");
                if (genreObj instanceof String) {
                    existingAnime.setGenre((String) genreObj);
                } else if (genreObj instanceof Integer) {
                    existingAnime.setGenre(String.valueOf(genreObj));
                }
                // 处理status字段，支持字符串和整数类型
                Object statusObj = request.get("status");
                if (statusObj instanceof Integer) {
                    existingAnime.setStatus((Integer) statusObj);
                } else if (statusObj instanceof String) {
                    try {
                        existingAnime.setStatus(Integer.parseInt((String) statusObj));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                animeService.save(existingAnime);
                
                // 获取新的集数列表
                Object newEpisodesObj = request.get("episodes");
                List<Map<String, Object>> newEpisodes = new ArrayList<>();
                if (newEpisodesObj instanceof List) {
                    List<?> episodesList = (List<?>) newEpisodesObj;
                    for (Object episodeObj : episodesList) {
                        if (episodeObj instanceof Map) {
                            Map<?, ?> episodeMap = (Map<?, ?>) episodeObj;
                            Map<String, Object> episodeData = new HashMap<>();
                            Object episodeNumberObj = episodeMap.get("episodeNumber");
                            if (episodeNumberObj instanceof Integer) {
                                episodeData.put("episodeNumber", episodeNumberObj);
                            }
                            Object videoUrlObj = episodeMap.get("videoUrl");
                            if (videoUrlObj instanceof String) {
                                episodeData.put("videoUrl", videoUrlObj);
                            }
                            newEpisodes.add(episodeData);
                        }
                    }
                }
                
                // 获取旧的集数列表
                List<Episode> oldEpisodes = episodeService.getEpisodesByAnimeId(animeId);
                
                // 找出需要删除的旧集数（不在新集数列表中的集数）
                List<Episode> episodesToDelete = new ArrayList<>();
                for (Episode oldEpisode : oldEpisodes) {
                    boolean found = false;
                    if (!newEpisodes.isEmpty()) {
                        for (Map<String, Object> newEpisode : newEpisodes) {
                            Object episodeNumberObj = newEpisode.get("episodeNumber");
                            if (episodeNumberObj instanceof Integer && oldEpisode.getEpisodeNumber().equals(episodeNumberObj)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        episodesToDelete.add(oldEpisode);
                    }
                }
                
                // 删除需要删除的集数
                for (Episode episode : episodesToDelete) {
                    episodeService.delete(episode.getId());
                }
                
                // 找出需要添加的新集数（不在旧集数列表中的集数）
                List<Map<String, Object>> episodesToAdd = new ArrayList<>();
                if (!newEpisodes.isEmpty()) {
                    for (Map<String, Object> newEpisode : newEpisodes) {
                        boolean found = false;
                        for (Episode oldEpisode : oldEpisodes) {
                            Object episodeNumberObj = newEpisode.get("episodeNumber");
                            if (episodeNumberObj instanceof Integer && oldEpisode.getEpisodeNumber().equals(episodeNumberObj)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            episodesToAdd.add(newEpisode);
                        }
                    }
                }
                
                // 添加需要添加的新集数
                for (Map<String, Object> episodeData : episodesToAdd) {
                    Object episodeNumberObj = episodeData.get("episodeNumber");
                    Object videoUrlObj = episodeData.get("videoUrl");
                    if (episodeNumberObj instanceof Integer && videoUrlObj instanceof String) {
                        Episode episode = new Episode();
                        episode.setAnimeId(animeId);
                        episode.setEpisodeNumber((Integer) episodeNumberObj);
                        episode.setVideoUrl((String) videoUrlObj);
                        episodeService.save(episode);
                    }
                }
                
                // 更新需要更新的集数（在新旧集数列表中都存在的集数）
                if (!newEpisodes.isEmpty()) {
                    for (Map<String, Object> newEpisode : newEpisodes) {
                        Object episodeNumberObj = newEpisode.get("episodeNumber");
                        Object videoUrlObj = newEpisode.get("videoUrl");
                        if (episodeNumberObj instanceof Integer && videoUrlObj instanceof String) {
                            for (Episode oldEpisode : oldEpisodes) {
                                if (oldEpisode.getEpisodeNumber().equals(episodeNumberObj)) {
                                    // 更新集数信息
                                    oldEpisode.setVideoUrl((String) videoUrlObj);
                                    episodeService.save(oldEpisode);
                                    break;
                                }
                            }
                        }
                    }
                }
                
                response.put("code", 200);
                response.put("msg", "动漫更新成功");
            } else {
                response.put("code", 400);
                response.put("msg", "动漫不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "更新动漫失败: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/animes/delete")
    public Map<String, Object> deleteAnime(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = request.get("id");
            
            // 先删除动漫的所有集数
            List<Episode> episodes = episodeService.getEpisodesByAnimeId(animeId);
            for (Episode episode : episodes) {
                episodeService.delete(episode.getId());
            }
            
            // 再删除动漫
            animeService.deleteById(animeId);
            response.put("code", 200);
            response.put("msg", "动漫删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "删除动漫失败: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/animes/toggleStatus")
    public Map<String, Object> toggleAnimeStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = Long.parseLong(request.get("id").toString());
            Integer status = Integer.parseInt(request.get("status").toString());
            Anime anime = animeService.findById(animeId);
            if (anime != null) {
                anime.setStatus(status);
                animeService.save(anime);
                response.put("code", 200);
                response.put("msg", "动漫状态更新成功");
            } else {
                response.put("code", 400);
                response.put("msg", "动漫不存在");
            }
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "更新动漫状态失败");
        }
        return response;
    }

    // 论坛管理 API
    @PostMapping("/forum/posts")
    public Map<String, Object> getPosts() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Post> posts = postService.findAll();
            response.put("code", 200);
            response.put("data", posts);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取帖子列表失败");
        }
        return response;
    }

    @PostMapping("/forum/posts/search")
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

    @PostMapping("/forum/posts/delete")
    public Map<String, Object> deletePost(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long postId = request.get("id");
            postService.deleteById(postId);
            response.put("code", 200);
            response.put("msg", "帖子删除成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "删除帖子失败");
        }
        return response;
    }

    @PostMapping("/forum/posts/sort")
    public Map<String, Object> sortPosts(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String sortBy = request.get("sortBy");
            String direction = request.getOrDefault("direction", "desc");
            List<Post> posts;
            switch (sortBy) {
                case "comments":
                    posts = postService.getAllPosts();
                    // 按评论数排序
                    if ("desc".equals(direction)) {
                        posts.sort((p1, p2) -> Integer.compare(p2.getCommentCount(), p1.getCommentCount()));
                    } else {
                        posts.sort((p1, p2) -> Integer.compare(p1.getCommentCount(), p2.getCommentCount()));
                    }
                    break;
                case "likes":
                    posts = postService.getPostsByLikes();
                    if ("asc".equals(direction)) {
                        posts.sort((p1, p2) -> Integer.compare(p1.getLikeCount(), p2.getLikeCount()));
                    }
                    break;
                case "dislikes":
                    posts = postService.getPostsByDislikes();
                    if ("asc".equals(direction)) {
                        posts.sort((p1, p2) -> Integer.compare(p1.getDislikeCount(), p2.getDislikeCount()));
                    }
                    break;
                default:
                    posts = postService.getPostsByTime();
                    if ("asc".equals(direction)) {
                        posts.sort((p1, p2) -> p1.getCreateTime().compareTo(p2.getCreateTime()));
                    }
            }
            response.put("code", 200);
            response.put("data", posts);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "排序帖子失败");
        }
        return response;
    }

    @PostMapping("/forum/posts/update")
    public Map<String, Object> updatePost(@RequestBody Post post) {
        Map<String, Object> response = new HashMap<>();
        try {
            Post existingPost = postService.getPostById(post.getId());
            if (existingPost != null) {
                existingPost.setTitle(post.getTitle());
                existingPost.setContent(post.getContent());
                postService.updatePost(existingPost);
                response.put("code", 200);
                response.put("msg", "帖子更新成功");
            } else {
                response.put("code", 400);
                response.put("msg", "帖子不存在");
            }
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "更新帖子失败");
        }
        return response;
    }

    @PostMapping("/forum/comments/update")
    public Map<String, Object> updateComment(@RequestBody Comment comment) {
        Map<String, Object> response = new HashMap<>();
        try {
            Comment updatedComment = commentService.updateComment(comment.getId(), comment.getContent());
            if (updatedComment != null) {
                response.put("code", 200);
                response.put("msg", "评论更新成功");
                // 发送WebSocket消息，通知前端评论已更新
                commentWebSocketHandler.sendCommentUpdateMessage("comment_updated:" + comment.getId());
            } else {
                response.put("code", 400);
                response.put("msg", "评论不存在");
            }
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "更新评论失败");
        }
        return response;
    }

    @PostMapping("/forum/comments")
    public Map<String, Object> getComments(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long postId = request.get("postId");
            List<Comment> comments = commentService.findByPostId(postId);
            response.put("code", 200);
            response.put("data", comments);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取评论列表失败");
        }
        return response;
    }

    @PostMapping("/forum/comments/delete")
    public Map<String, Object> deleteComment(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long commentId = request.get("id");
            commentService.deleteById(commentId);
            response.put("code", 200);
            response.put("msg", "评论删除成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "删除评论失败");
        }
        return response;
    }

    // 已删除记录管理 API
    // 用户相关
    @PostMapping("/users/deleted")
    public Map<String, Object> getDeletedUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<User> users = userService.findAllDeleted();
            response.put("code", 200);
            response.put("data", users);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取已删除用户列表失败");
        }
        return response;
    }

    @PostMapping("/users/restore")
    public Map<String, Object> restoreUser(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = request.get("id");
            userService.restore(userId);
            response.put("code", 200);
            response.put("msg", "用户恢复成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "恢复用户失败");
        }
        return response;
    }

    @PostMapping("/users/hardDelete")
    public Map<String, Object> hardDeleteUser(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = request.get("id");
            userService.hardDelete(userId);
            response.put("code", 200);
            response.put("msg", "用户彻底删除成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "彻底删除用户失败");
        }
        return response;
    }

    // 动漫相关
    @PostMapping("/animes/deleted")
    public Map<String, Object> getDeletedAnimes() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Anime> animes = animeService.findAllDeleted();
            response.put("code", 200);
            response.put("data", animes);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取已删除动漫列表失败");
        }
        return response;
    }

    @PostMapping("/animes/restore")
    public Map<String, Object> restoreAnime(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = request.get("id");
            animeService.restore(animeId);
            response.put("code", 200);
            response.put("msg", "动漫恢复成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "恢复动漫失败");
        }
        return response;
    }

    @PostMapping("/animes/hardDelete")
    public Map<String, Object> hardDeleteAnime(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long animeId = request.get("id");
            animeService.hardDelete(animeId);
            response.put("code", 200);
            response.put("msg", "动漫彻底删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "彻底删除动漫失败: " + e.getMessage());
        }
        return response;
    }

    // 集数相关
    @PostMapping("/episodes/deleted")
    public Map<String, Object> getDeletedEpisodes() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Episode> episodes = episodeService.findAllDeleted();
            // 为每个集数添加对应的动漫标题和封面
            List<Map<String, Object>> episodesWithAnimeTitle = new ArrayList<>();
            for (Episode episode : episodes) {
                Map<String, Object> episodeMap = new HashMap<>();
                episodeMap.put("id", episode.getId());
                episodeMap.put("animeId", episode.getAnimeId());
                episodeMap.put("episodeNumber", episode.getEpisodeNumber());
                episodeMap.put("videoUrl", episode.getVideoUrl());
                episodeMap.put("deleted", episode.getDeleted());
                episodeMap.put("deletedAt", episode.getDeletedAt());
                // 获取动漫标题和封面（包括已删除的动漫）
                Anime anime = animeService.findByIdIncludingDeleted(episode.getAnimeId());
                if (anime != null) {
                    episodeMap.put("animeTitle", anime.getTitle());
                    episodeMap.put("animeImage", anime.getImage());
                } else {
                    episodeMap.put("animeTitle", "未知动漫");
                    episodeMap.put("animeImage", null);
                }
                episodesWithAnimeTitle.add(episodeMap);
            }
            response.put("code", 200);
            response.put("data", episodesWithAnimeTitle);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "获取已删除集数列表失败");
        }
        return response;
    }

    @PostMapping("/episodes/restore")
    public Map<String, Object> restoreEpisode(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long episodeId = request.get("id");
            // 恢复集数
            episodeService.restore(episodeId);
            // 从数据库中获取恢复后的集数信息，以便获取动漫ID
            Episode episode = episodeService.findById(episodeId);
            if (episode != null) {
                // 检查并恢复对应的动漫
                Long animeId = episode.getAnimeId();
                Anime anime = animeService.findByIdIncludingDeleted(animeId);
                if (anime != null && anime.getDeleted() != null && anime.getDeleted()) {
                    animeService.restore(animeId);
                }
            }
            response.put("code", 200);
            response.put("msg", "集数恢复成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "恢复集数失败");
        }
        return response;
    }

    @PostMapping("/episodes/hardDelete")
    public Map<String, Object> hardDeleteEpisode(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long episodeId = request.get("id");
            episodeService.hardDelete(episodeId);
            response.put("code", 200);
            response.put("msg", "集数彻底删除成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "彻底删除集数失败");
        }
        return response;
    }
    
    // 视频上传 API
    @PostMapping("/upload/video")
    public Map<String, Object> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("开始上传视频...");
            System.out.println("文件名: " + file.getOriginalFilename());
            System.out.println("文件大小: " + file.getSize() + " bytes");
            System.out.println("目标路径: " + fileName);
            
            // 使用OSSUtil上传视频
            String videoUrl = ossUtil.uploadVideo(file, fileName);
            System.out.println("上传成功，URL: " + videoUrl);
            
            response.put("code", 200);
            response.put("data", videoUrl);
            response.put("msg", "视频上传成功");
            return response;
        } catch (Exception e) {
            System.out.println("上传失败:");
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "视频上传失败: " + e.getMessage());
            return response;
        }
  }


}