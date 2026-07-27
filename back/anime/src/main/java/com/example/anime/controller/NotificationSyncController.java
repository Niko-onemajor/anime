package com.example.anime.controller;

import com.example.anime.service.NotificationSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationSyncController {

    @Autowired
    private NotificationSyncService notificationSyncService;

    @PostMapping("/sync")
    public Map<String, Object> syncNotifications() {
        return notificationSyncService.syncAll();
    }
}