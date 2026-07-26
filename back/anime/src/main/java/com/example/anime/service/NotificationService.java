package com.example.anime.service;

import com.example.anime.model.Notification;
import com.example.anime.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(Long userId, String username, String type, String message) {
        System.out.println("NotificationService.createNotification 被调用");
        System.out.println("  userId=" + userId + ", username=" + username + ", type=" + type);
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUsername(username);
        notification.setType(type);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreateTime(new Date());
        Notification saved = notificationRepository.save(notification);
        System.out.println("  通知已保存, id=" + saved.getId());
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
                                 String postTitle, String replyContent) {
        String message = String.format("用户 %s 回复了你在论坛帖子「%s」中的评论：%s",
                fromUsername, postTitle, truncateContent(replyContent, 50));
        createNotification(targetUserId, targetUsername, "FORUM_REPLY", message);
    }

    /**
     * 论坛评论被点赞通知
     */
    public void notifyForumLike(Long targetUserId, String targetUsername, String fromUsername, String postTitle) {
        String message = String.format("用户 %s 赞了你在论坛帖子「%s」中的评论",
                fromUsername, postTitle);
        createNotification(targetUserId, targetUsername, "FORUM_LIKE", message);
    }

    /**
     * 动漫评论被回复通知
     */
    public void notifyAnimeReply(Long targetUserId, String targetUsername, String fromUsername,
                                 String animeTitle, String replyContent) {
        String message = String.format("用户 %s 回复了你在动漫「%s」中的评论：%s",
                fromUsername, animeTitle, truncateContent(replyContent, 50));
        createNotification(targetUserId, targetUsername, "ANIME_REPLY", message);
    }

    /**
     * 动漫评论被点赞通知
     */
    public void notifyAnimeLike(Long targetUserId, String targetUsername, String fromUsername, String animeTitle) {
        String message = String.format("用户 %s 赞了你在动漫「%s」中的评论",
                fromUsername, animeTitle);
        createNotification(targetUserId, targetUsername, "ANIME_LIKE", message);
    }
}