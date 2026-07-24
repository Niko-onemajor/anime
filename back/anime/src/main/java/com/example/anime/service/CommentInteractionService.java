package com.example.anime.service;

import com.example.anime.model.CommentInteraction;
import com.example.anime.repository.CommentInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentInteractionService {
    @Autowired
    private CommentInteractionRepository commentInteractionRepository;

    // 根据用户ID获取互动记录
    public List<CommentInteraction> getByUserId(Long userId) {
        return commentInteractionRepository.findAll().stream()
                .filter(interaction -> interaction.getUserId().equals(userId))
                .collect(java.util.stream.Collectors.toList());
    }

    // 根据评论ID获取互动记录
    public List<CommentInteraction> getByCommentId(Long commentId) {
        return commentInteractionRepository.findAll().stream()
                .filter(interaction -> interaction.getCommentId().equals(commentId))
                .collect(java.util.stream.Collectors.toList());
    }

    // 根据ID删除互动记录
    public void deleteById(Long id) {
        commentInteractionRepository.deleteById(id);
    }
}