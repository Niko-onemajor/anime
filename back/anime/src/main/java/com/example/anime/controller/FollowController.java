package com.example.anime.controller;

import com.example.anime.model.User;
import com.example.anime.service.FollowService;
import com.example.anime.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
public class FollowController {
    @Autowired
    private FollowService followService;

    @Autowired
    private UserRepository userRepository;

    // 切换关注状态
    @PostMapping("/toggle")
    public Map<String, Object> toggleFollow(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        Long followerId = Long.valueOf(request.get("followerId").toString());
        Long followedId = Long.valueOf(request.get("followedId").toString());

        if (followerId.equals(followedId)) {
            response.put("code", 400);
            response.put("msg", "不能关注自己");
            return response;
        }

        boolean followed = followService.follow(followerId, followedId);
        response.put("code", 200);
        response.put("msg", followed ? "关注成功" : "已取消关注");
        Map<String, Object> data = new HashMap<>();
        data.put("followed", followed);
        response.put("data", data);
        return response;
    }

    // 检查关注状态
    @GetMapping("/status")
    public Map<String, Object> getFollowStatus(@RequestParam Long followerId, @RequestParam Long followedId) {
        Map<String, Object> response = new HashMap<>();
        boolean following = followService.isFollowing(followerId, followedId);
        response.put("code", 200);
        Map<String, Object> data = new HashMap<>();
        data.put("following", following);
        response.put("data", data);
        return response;
    }

    // 获取粉丝数
    @GetMapping("/follower-count")
    public Map<String, Object> getFollowerCount(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        long count = followService.getFollowerCount(userId);
        response.put("code", 200);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        response.put("data", data);
        return response;
    }

    // 获取关注数
    @GetMapping("/following-count")
    public Map<String, Object> getFollowingCount(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        long count = followService.getFollowingCount(userId);
        response.put("code", 200);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        response.put("data", data);
        return response;
    }

    // 获取我关注的人列表
    @GetMapping("/following-list")
    public Map<String, Object> getFollowingList(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        List<User> users = followService.getFollowingList(userId);
        List<Map<String, Object>> userList = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("avatar", user.getAvatar());
            userMap.put("role", user.getRole());
            userMap.put("signature", user.getSignature());
            userList.add(userMap);
        }
        response.put("code", 200);
        response.put("data", userList);
        return response;
    }

    // 获取我的粉丝列表
    @GetMapping("/follower-list")
    public Map<String, Object> getFollowerList(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        List<User> users = followService.getFollowerList(userId);
        List<Map<String, Object>> userList = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("avatar", user.getAvatar());
            userMap.put("role", user.getRole());
            userMap.put("signature", user.getSignature());
            userList.add(userMap);
        }
        response.put("code", 200);
        response.put("data", userList);
        return response;
    }
}