package com.example.anime.controller;

import com.example.anime.model.ChatMessage;
import com.example.anime.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatMessageController {
    @Autowired
    private ChatMessageService chatMessageService;

    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        Long senderId = request.get("senderId") != null ? Long.valueOf(request.get("senderId").toString()) : null;
        Long receiverId = request.get("receiverId") != null ? Long.valueOf(request.get("receiverId").toString()) : null;
        String content = (String) request.get("content");

        if (senderId == null || receiverId == null || content == null || content.trim().isEmpty()) {
            response.put("code", 400);
            response.put("msg", "参数不完整");
            return response;
        }

        try {
            ChatMessage message = chatMessageService.sendMessage(senderId, receiverId, content);
            Map<String, Object> data = new HashMap<>();
            data.put("id", message.getId());
            data.put("senderId", message.getSenderId());
            data.put("receiverId", message.getReceiverId());
            data.put("content", message.getContent());
            data.put("createTime", message.getCreateTime());
            response.put("code", 200);
            response.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "发送失败: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/conversation")
    public Map<String, Object> getConversation(@RequestParam Long userId1, @RequestParam Long userId2) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ChatMessage> messages = chatMessageService.getConversation(userId1, userId2);
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (ChatMessage msg : messages) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", msg.getId());
                item.put("senderId", msg.getSenderId());
                item.put("receiverId", msg.getReceiverId());
                item.put("content", msg.getContent());
                item.put("isRead", msg.getIsRead());
                item.put("createTime", msg.getCreateTime());
                dataList.add(item);
            }
            response.put("code", 200);
            response.put("data", dataList);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "获取失败: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/conversations")
    public Map<String, Object> getConversationList(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Map<String, Object>> list = chatMessageService.getConversationList(userId);
            response.put("code", 200);
            response.put("data", list);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "获取失败: " + e.getMessage());
        }

        return response;
    }

    @PostMapping("/mark-read")
    public Map<String, Object> markAllAsRead(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        Long senderId = request.get("senderId") != null ? Long.valueOf(request.get("senderId").toString()) : null;
        Long receiverId = request.get("receiverId") != null ? Long.valueOf(request.get("receiverId").toString()) : null;

        if (senderId == null || receiverId == null) {
            response.put("code", 400);
            response.put("msg", "参数不完整");
            return response;
        }

        try {
            chatMessageService.markAllAsRead(senderId, receiverId);
            response.put("code", 200);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "标记失败: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            long count = chatMessageService.getUnreadCount(userId);
            response.put("code", 200);
            response.put("unreadCount", count);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "获取失败: " + e.getMessage());
        }

        return response;
    }
}