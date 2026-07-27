package com.example.anime.repository;

import com.example.anime.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByCreateTimeAsc(
            Long senderId, Long receiverId, Long receiverId2, Long senderId2);

    List<ChatMessage> findByReceiverIdAndIsReadFalse(Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    List<ChatMessage> findBySenderIdOrReceiverIdOrderByCreateTimeDesc(Long senderId, Long receiverId);
}