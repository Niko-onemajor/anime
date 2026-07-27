package com.example.anime.controller;

import com.example.anime.model.Notification;
import com.example.anime.model.User;
import com.example.anime.service.NotificationService;
import com.example.anime.service.NotificationSyncService;
import com.example.anime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationSyncService notificationSyncService;

    @GetMapping("/list")
    public Map<String, Object> getNotifications(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findByUsername(username);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }
            List<Notification> notifications = notificationService.getNotificationsByUserId(user.getId());
            long unreadCount = notificationService.getUnreadCount(user.getId());
            response.put("code", 200);
            response.put("data", notifications);
            response.put("unreadCount", unreadCount);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "获取通知失败");
        }
        return response;
    }

    @PostMapping("/read")
    public Map<String, Object> markAsRead(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long notificationId = request.get("id");
            notificationService.markAsRead(notificationId);
            response.put("code", 200);
            response.put("msg", "已标记为已读");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "操作失败");
        }
        return response;
    }

    @PostMapping("/read-all")
    public Map<String, Object> markAllAsRead(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = request.get("username");
            User user = userService.findByUsername(username);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }
            notificationService.markAllAsRead(user.getId());
            response.put("code", 200);
            response.put("msg", "全部已读");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "操作失败");
        }
        return response;
    }

    @PostMapping("/clear-all")
    public Map<String, Object> clearAll(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = request.get("username");
            User user = userService.findByUsername(username);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }
            notificationService.clearAll(user.getId());
            response.put("code", 200);
            response.put("msg", "已清除全部通知");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "操作失败");
        }
        return response;
    }

    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findByUsername(username);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }
            long unreadCount = notificationService.getUnreadCount(user.getId());
            response.put("code", 200);
            response.put("unreadCount", unreadCount);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "获取未读数失败");
        }
        return response;
    }

    @PostMapping("/sync")
    public Map<String, Object> syncNotifications() {
        return notificationSyncService.syncAll();
    }
}