package com.example.anime.service;

import com.example.anime.model.ForumCommentInteraction;
import com.example.anime.repository.ForumCommentInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForumCommentInteractionService {
    @Autowired
    private ForumCommentInteractionRepository forumCommentInteractionRepository;

    // 根据用户ID获取互动记录
    public List<ForumCommentInteraction> getByUserId(Long userId) {
        return forumCommentInteractionRepository.findAll().stream()
                .filter(interaction -> interaction.getUserId().equals(userId))
                .collect(java.util.stream.Collectors.toList());
    }

    // 根据评论ID获取互动记录
    public List<ForumCommentInteraction> getByCommentId(Long commentId) {
        return forumCommentInteractionRepository.findAll().stream()
                .filter(interaction -> interaction.getCommentId().equals(commentId))
                .collect(java.util.stream.Collectors.toList());
    }

    // 根据ID删除互动记录
    public void deleteById(Long id) {
        forumCommentInteractionRepository.deleteById(id);
    }
}