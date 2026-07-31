package com.example.anime.service;

import com.example.anime.model.ChatMessage;
import com.example.anime.model.User;
import com.example.anime.repository.ChatMessageRepository;
import com.example.anime.repository.UserRepository;
import com.example.anime.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class ChatMessageService {
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    public ChatMessage sendMessage(Long senderId, Long receiverId, String content) {
        ChatMessage message = new ChatMessage(senderId, receiverId, content);
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getConversation(Long userId1, Long userId2) {
        return chatMessageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByCreateTimeAsc(
                userId1, userId2, userId1, userId2);
    }

    public List<Map<String, Object>> getConversationList(Long userId) {
        List<ChatMessage> messages = chatMessageRepository.findBySenderIdOrReceiverIdOrderByCreateTimeDesc(userId, userId);
        Map<Long, Map<String, Object>> conversationMap = new LinkedHashMap<>();

        for (ChatMessage msg : messages) {
            Long otherUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();

            if (!conversationMap.containsKey(otherUserId)) {
                Map<String, Object> conv = new HashMap<>();
                User otherUser = userRepository.findByIdAndDeletedFalse(otherUserId);
                // 非管理员不显示测试用户
                if (otherUser != null && Boolean.TRUE.equals(otherUser.getIsTest())
                        && !SecurityUtils.isCurrentUserAdmin()) {
                    continue;
                }
                conv.put("userId", otherUserId);
                conv.put("username", otherUser != null ? otherUser.getUsername() : "未知用户");
                conv.put("avatar", otherUser != null ? otherUser.getAvatar() : "");
                conv.put("lastMessage", msg.getContent());
                conv.put("lastMessageTime", msg.getCreateTime());
                conv.put("unreadCount", 0);
                conversationMap.put(otherUserId, conv);
            }

            // 统计未读消息数（仅对方发给当前用户的消息）
            Map<String, Object> conv = conversationMap.get(otherUserId);
            if (msg.getSenderId().equals(otherUserId) && !msg.getIsRead()) {
                conv.put("unreadCount", (Integer) conv.get("unreadCount") + 1);
            }
        }

        return new ArrayList<>(conversationMap.values());
    }

    public void markAsRead(Long messageId) {
        Optional<ChatMessage> opt = chatMessageRepository.findById(messageId);
        if (opt.isPresent()) {
            ChatMessage msg = opt.get();
            msg.setIsRead(true);
            chatMessageRepository.save(msg);
        }
    }

    @Transactional
    public void markAllAsRead(Long senderId, Long receiverId) {
        List<ChatMessage> messages = chatMessageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByCreateTimeAsc(
                senderId, receiverId, senderId, receiverId);
        for (ChatMessage msg : messages) {
            if (msg.getReceiverId().equals(receiverId) && !msg.getIsRead()) {
                msg.setIsRead(true);
                chatMessageRepository.save(msg);
            }
        }
    }

    public long getUnreadCount(Long userId) {
        return chatMessageRepository.countByReceiverIdAndIsReadFalse(userId);
    }
}