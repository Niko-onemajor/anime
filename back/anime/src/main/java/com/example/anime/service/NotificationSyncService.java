package com.example.anime.service;

import com.example.anime.model.*;
import com.example.anime.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NotificationSyncService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AnimeCommentRepository animeCommentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumCommentInteractionRepository forumCommentInteractionRepository;

    @Autowired
    private CommentInteractionRepository commentInteractionRepository;

    @Autowired
    private ForumPostInteractionRepository forumPostInteractionRepository;

    private int syncCount = 0;

    public Map<String, Object> syncAll() {
        syncCount = 0;
        Map<String, Object> result = new HashMap<>();

        try {
            syncForumCommentReplies();
            syncForumTopLevelComments();
            syncForumCommentLikes();
            syncForumCommentDislikes();
            syncAnimeCommentReplies();
            syncAnimeCommentLikes();
            syncAnimeCommentDislikes();
            syncPostLikes();
            syncPostDislikes();

            result.put("code", 200);
            result.put("msg", "同步完成，新增 " + syncCount + " 条通知");
            result.put("count", syncCount);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "同步失败: " + e.getMessage());
        }
        return result;
    }

    private void createIfNotExists(Long userId, String username, String type, String message, Date createTime,
                                     Long targetId, String targetType, Long subTargetId) {
        if (!notificationRepository.existsByUserIdAndTypeAndMessage(userId, type, message)) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setUsername(username);
            notification.setType(type);
            notification.setMessage(message);
            notification.setIsRead(false);
            notification.setCreateTime(createTime != null ? createTime : new Date());
            notification.setTargetId(targetId);
            notification.setTargetType(targetType);
            notification.setSubTargetId(subTargetId);
            notificationRepository.save(notification);
            syncCount++;
        }
    }

    // 1. 论坛评论回复 → FORUM_REPLY
    private void syncForumCommentReplies() {
        List<Comment> allComments = commentRepository.findAll();
        for (Comment comment : allComments) {
            if (comment.getParentId() != null) {
                // 这是一个回复，通知父评论作者
                Comment parentComment = commentRepository.findById(comment.getParentId()).orElse(null);
                if (parentComment != null && !parentComment.getAuthorId().equals(comment.getAuthorId())) {
                    User targetUser = userRepository.findById(parentComment.getAuthorId()).orElse(null);
                    User fromUser = userRepository.findById(comment.getAuthorId()).orElse(null);
                    Post post = postRepository.findById(comment.getPostId()).orElse(null);
                    if (targetUser != null && fromUser != null && post != null) {
                        String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                        String message = String.format("用户 %s 回复了你在论坛帖子「%s」中的评论：%s",
                                fromUser.getUsername(), postTitle, truncateContent(comment.getContent(), 50));
                        createIfNotExists(targetUser.getId(), targetUser.getUsername(), "FORUM_REPLY", message, comment.getCreateTime(), post.getId(), "forum", comment.getId());
                    }
                }
            }
        }
    }

    // 2. 论坛顶级评论 → FORUM_REPLY (通知帖子作者)
    private void syncForumTopLevelComments() {
        List<Comment> allComments = commentRepository.findAll();
        for (Comment comment : allComments) {
            if (comment.getParentId() == null) {
                Post post = postRepository.findById(comment.getPostId()).orElse(null);
                if (post != null && post.getAuthor() != null && !post.getAuthor().getId().equals(comment.getAuthorId())) {
                    User fromUser = userRepository.findById(comment.getAuthorId()).orElse(null);
                    if (fromUser != null) {
                        String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                        String message = String.format("用户 %s 评论了你的论坛帖子「%s」：%s",
                                fromUser.getUsername(), postTitle, truncateContent(comment.getContent(), 50));
                        createIfNotExists(post.getAuthor().getId(), post.getAuthor().getUsername(), "FORUM_REPLY", message, comment.getCreateTime(), post.getId(), "forum", comment.getId());
                    }
                }
            }
        }
    }

    // 3. 论坛评论点赞 → FORUM_LIKE
    private void syncForumCommentLikes() {
        List<ForumCommentInteraction> interactions = forumCommentInteractionRepository.findAll();
        for (ForumCommentInteraction interaction : interactions) {
            if (interaction.getInteractionType() == 1) {
                Comment comment = commentRepository.findById(interaction.getCommentId()).orElse(null);
                if (comment != null && !comment.getAuthorId().equals(interaction.getUserId())) {
                    User targetUser = userRepository.findById(comment.getAuthorId()).orElse(null);
                    User fromUser = userRepository.findById(interaction.getUserId()).orElse(null);
                    Post post = postRepository.findById(comment.getPostId()).orElse(null);
                    if (targetUser != null && fromUser != null && post != null) {
                        String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                        String message = String.format("用户 %s 赞了你在论坛帖子「%s」中的评论",
                                fromUser.getUsername(), postTitle);
                        createIfNotExists(targetUser.getId(), targetUser.getUsername(), "FORUM_LIKE", message, interaction.getCreateTime(), post.getId(), "forum", interaction.getCommentId());
                    }
                }
            }
        }
    }

    // 4. 论坛评论点踩 → FORUM_DISLIKE
    private void syncForumCommentDislikes() {
        List<ForumCommentInteraction> interactions = forumCommentInteractionRepository.findAll();
        for (ForumCommentInteraction interaction : interactions) {
            if (interaction.getInteractionType() == 2) {
                Comment comment = commentRepository.findById(interaction.getCommentId()).orElse(null);
                if (comment != null && !comment.getAuthorId().equals(interaction.getUserId())) {
                    User targetUser = userRepository.findById(comment.getAuthorId()).orElse(null);
                    User fromUser = userRepository.findById(interaction.getUserId()).orElse(null);
                    Post post = postRepository.findById(comment.getPostId()).orElse(null);
                    if (targetUser != null && fromUser != null && post != null) {
                        String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                        String message = String.format("用户 %s 踩了你在论坛帖子「%s」中的评论",
                                fromUser.getUsername(), postTitle);
                        createIfNotExists(targetUser.getId(), targetUser.getUsername(), "FORUM_DISLIKE", message, interaction.getCreateTime(), post.getId(), "forum", interaction.getCommentId());
                    }
                }
            }
        }
    }

    // 5. 动漫评论回复 → ANIME_REPLY
    private void syncAnimeCommentReplies() {
        List<AnimeComment> allComments = animeCommentRepository.findAll();
        for (AnimeComment comment : allComments) {
            if (comment.getParentId() != null) {
                AnimeComment parentComment = animeCommentRepository.findById(comment.getParentId()).orElse(null);
                if (parentComment != null && !parentComment.getAuthorId().equals(comment.getAuthorId())) {
                    User targetUser = userRepository.findById(parentComment.getAuthorId()).orElse(null);
                    User fromUser = userRepository.findById(comment.getAuthorId()).orElse(null);
                    Anime anime = animeRepository.findById(comment.getAnimeId()).orElse(null);
                    if (targetUser != null && fromUser != null && anime != null) {
                        String animeTitle = anime.getTitle() != null ? anime.getTitle() : "未知动漫";
                        String message = String.format("用户 %s 回复了你在动漫「%s」中的评论：%s",
                                fromUser.getUsername(), animeTitle, truncateContent(comment.getContent(), 50));
                        createIfNotExists(targetUser.getId(), targetUser.getUsername(), "ANIME_REPLY", message, comment.getCreateTime(), anime.getId(), "anime", comment.getId());
                    }
                }
            }
        }
    }

    // 6. 动漫评论点赞 → ANIME_LIKE
    private void syncAnimeCommentLikes() {
        List<CommentInteraction> interactions = commentInteractionRepository.findAll();
        for (CommentInteraction interaction : interactions) {
            if (interaction.getInteractionType() == 1) {
                AnimeComment comment = animeCommentRepository.findById(interaction.getCommentId()).orElse(null);
                if (comment != null && !comment.getAuthorId().equals(interaction.getUserId())) {
                    User targetUser = userRepository.findById(comment.getAuthorId()).orElse(null);
                    User fromUser = userRepository.findById(interaction.getUserId()).orElse(null);
                    Anime anime = animeRepository.findById(comment.getAnimeId()).orElse(null);
                    if (targetUser != null && fromUser != null && anime != null) {
                        String animeTitle = anime.getTitle() != null ? anime.getTitle() : "未知动漫";
                        String message = String.format("用户 %s 赞了你在动漫「%s」中的评论",
                                fromUser.getUsername(), animeTitle);
                        createIfNotExists(targetUser.getId(), targetUser.getUsername(), "ANIME_LIKE", message, interaction.getCreateTime(), comment.getAnimeId(), "anime", interaction.getCommentId());
                    }
                }
            }
        }
    }

    // 7. 动漫评论点踩 → ANIME_DISLIKE
    private void syncAnimeCommentDislikes() {
        List<CommentInteraction> interactions = commentInteractionRepository.findAll();
        for (CommentInteraction interaction : interactions) {
            if (interaction.getInteractionType() == 2) {
                AnimeComment comment = animeCommentRepository.findById(interaction.getCommentId()).orElse(null);
                if (comment != null && !comment.getAuthorId().equals(interaction.getUserId())) {
                    User targetUser = userRepository.findById(comment.getAuthorId()).orElse(null);
                    User fromUser = userRepository.findById(interaction.getUserId()).orElse(null);
                    Anime anime = animeRepository.findById(comment.getAnimeId()).orElse(null);
                    if (targetUser != null && fromUser != null && anime != null) {
                        String animeTitle = anime.getTitle() != null ? anime.getTitle() : "未知动漫";
                        String message = String.format("用户 %s 踩了你在动漫「%s」中的评论",
                                fromUser.getUsername(), animeTitle);
                        createIfNotExists(targetUser.getId(), targetUser.getUsername(), "ANIME_DISLIKE", message, interaction.getCreateTime(), comment.getAnimeId(), "anime", interaction.getCommentId());
                    }
                }
            }
        }
    }

    // 8. 帖子点赞 → FORUM_LIKE
    private void syncPostLikes() {
        List<ForumPostInteraction> interactions = forumPostInteractionRepository.findAll();
        for (ForumPostInteraction interaction : interactions) {
            if (interaction.getInteractionType() == 1) {
                Post post = postRepository.findById(interaction.getPostId()).orElse(null);
                if (post != null && post.getAuthor() != null && !post.getAuthor().getId().equals(interaction.getUserId())) {
                    User fromUser = userRepository.findById(interaction.getUserId()).orElse(null);
                    if (fromUser != null) {
                        String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                        String message = String.format("用户 %s 赞了你的论坛帖子「%s」",
                                fromUser.getUsername(), postTitle);
                        createIfNotExists(post.getAuthor().getId(), post.getAuthor().getUsername(), "FORUM_LIKE", message, interaction.getCreateTime(), post.getId(), "forum", null);
                    }
                }
            }
        }
    }

    // 9. 帖子点踩 → FORUM_DISLIKE
    private void syncPostDislikes() {
        List<ForumPostInteraction> interactions = forumPostInteractionRepository.findAll();
        for (ForumPostInteraction interaction : interactions) {
            if (interaction.getInteractionType() == 2) {
                Post post = postRepository.findById(interaction.getPostId()).orElse(null);
                if (post != null && post.getAuthor() != null && !post.getAuthor().getId().equals(interaction.getUserId())) {
                    User fromUser = userRepository.findById(interaction.getUserId()).orElse(null);
                    if (fromUser != null) {
                        String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                        String message = String.format("用户 %s 踩了你的论坛帖子「%s」",
                                fromUser.getUsername(), postTitle);
                        createIfNotExists(post.getAuthor().getId(), post.getAuthor().getUsername(), "FORUM_DISLIKE", message, interaction.getCreateTime(), post.getId(), "forum", null);
                    }
                }
            }
        }
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        String trimmed = content.trim();
        if (trimmed.length() <= maxLength) return trimmed;
        return trimmed.substring(0, maxLength) + "...";
    }
}