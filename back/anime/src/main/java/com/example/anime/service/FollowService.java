package com.example.anime.service;

import com.example.anime.model.Follow;
import com.example.anime.model.User;
import com.example.anime.repository.FollowRepository;
import com.example.anime.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class FollowService {
    @Autowired
    private FollowRepository followRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // 切换关注状态：关注则返回true，取消关注则返回false
    @Transactional
    public boolean follow(Long followerId, Long followedId) {
        if (followRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            followRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
            return false;
        } else {
            Follow follow = new Follow(followerId, followedId);
            followRepository.save(follow);
            return true;
        }
    }
    
    // 检查是否已关注
    public boolean isFollowing(Long followerId, Long followedId) {
        return followRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }
    
    // 获取粉丝数
    public long getFollowerCount(Long userId) {
        return followRepository.countByFollowedId(userId);
    }
    
    // 获取关注数
    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }
    
    // 获取我关注的人列表
    public List<User> getFollowingList(Long userId) {
        List<Follow> follows = followRepository.findByFollowerId(userId);
        List<User> users = new ArrayList<>();
        for (Follow follow : follows) {
            User user = userRepository.findByIdAndDeletedFalse(follow.getFollowedId());
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }
    
    // 获取我的粉丝列表
    public List<User> getFollowerList(Long userId) {
        List<Follow> follows = followRepository.findByFollowedId(userId);
        List<User> users = new ArrayList<>();
        for (Follow follow : follows) {
            User user = userRepository.findByIdAndDeletedFalse(follow.getFollowerId());
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }
}