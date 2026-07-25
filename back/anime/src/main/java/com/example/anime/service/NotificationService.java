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
}