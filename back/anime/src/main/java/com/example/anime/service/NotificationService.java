package com.example.anime.service;

import com.example.anime.model.Notification;
import com.example.anime.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(Long userId, String username, String type, String message,
                                            Long targetId, String targetType, Long subTargetId) {
        log.debug("创建通知: userId={}, username={}, type={}", userId, username, type);
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUsername(username);
        notification.setType(type);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreateTime(new Date());
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setSubTargetId(subTargetId);
        Notification saved = notificationRepository.save(notification);
        log.debug("通知已保存, id={}", saved.getId());
        return saved;
    }

    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    public void clearAll(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    /**
     * 删除与指定评论相关的所有通知
     */
    public void deleteByCommentId(Long commentId) {
        notificationRepository.deleteBySubTargetId(commentId);
    }

    /**
     * 删除与指定目标（帖子/动漫）相关的所有通知
     */
    public void deleteByTarget(String targetType, Long targetId) {
        notificationRepository.deleteByTargetTypeAndTargetId(targetType, targetId);
    }

    /**
     * 截断文本，避免通知内容过长
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    /**
     * 论坛评论被回复通知
     */
    public void notifyForumReply(Long targetUserId, String targetUsername, String fromUsername,
                                 String postTitle, String replyContent, Long postId, Long commentId) {
        String message = String.format("用户 %s 回复了你在论坛帖子「%s」中的评论：%s",
                fromUsername, postTitle, truncateContent(replyContent, 50));
        createNotification(targetUserId, targetUsername, "FORUM_REPLY", message, postId, "forum", commentId);
    }

    /**
     * 论坛评论被点赞通知
     */
    public void notifyForumLike(Long targetUserId, String targetUsername, String fromUsername, String postTitle, Long postId, Long commentId) {
        String message = String.format("用户 %s 赞了你在论坛帖子「%s」中的评论",
                fromUsername, postTitle);
        createNotification(targetUserId, targetUsername, "FORUM_LIKE", message, postId, "forum", commentId);
    }

    /**
     * 动漫评论被回复通知
     */
    public void notifyAnimeReply(Long targetUserId, String targetUsername, String fromUsername,
                                 String animeTitle, String replyContent, Long animeId, Long commentId) {
        String message = String.format("用户 %s 回复了你在动漫「%s」中的评论：%s",
                fromUsername, animeTitle, truncateContent(replyContent, 50));
        createNotification(targetUserId, targetUsername, "ANIME_REPLY", message, animeId, "anime", commentId);
    }

    /**
     * 动漫评论被点赞通知
     */
    public void notifyAnimeLike(Long targetUserId, String targetUsername, String fromUsername, String animeTitle, Long animeId, Long commentId) {
        String message = String.format("用户 %s 赞了你在动漫「%s」中的评论",
                fromUsername, animeTitle);
        createNotification(targetUserId, targetUsername, "ANIME_LIKE", message, animeId, "anime", commentId);
    }

    /**
     * 论坛帖子被点赞通知
     */
    public void notifyPostLike(Long targetUserId, String targetUsername, String fromUsername, String postTitle, Long postId) {
        String message = String.format("用户 %s 赞了你的论坛帖子「%s」",
                fromUsername, postTitle);
        createNotification(targetUserId, targetUsername, "FORUM_LIKE", message, postId, "forum", null);
    }

    /**
     * 论坛评论被点踩通知
     */
    public void notifyForumDislike(Long targetUserId, String targetUsername, String fromUsername, String postTitle, Long postId, Long commentId) {
        String message = String.format("用户 %s 踩了你在论坛帖子「%s」中的评论",
                fromUsername, postTitle);
        createNotification(targetUserId, targetUsername, "FORUM_DISLIKE", message, postId, "forum", commentId);
    }

    /**
     * 动漫评论被点踩通知
     */
    public void notifyAnimeDislike(Long targetUserId, String targetUsername, String fromUsername, String animeTitle, Long animeId, Long commentId) {
        String message = String.format("用户 %s 踩了你在动漫「%s」中的评论",
                fromUsername, animeTitle);
        createNotification(targetUserId, targetUsername, "ANIME_DISLIKE", message, animeId, "anime", commentId);
    }

    /**
     * 论坛帖子被点踩通知
     */
    public void notifyPostDislike(Long targetUserId, String targetUsername, String fromUsername, String postTitle, Long postId) {
        String message = String.format("用户 %s 踩了你的论坛帖子「%s」",
                fromUsername, postTitle);
        createNotification(targetUserId, targetUsername, "FORUM_DISLIKE", message, postId, "forum", null);
    }
}