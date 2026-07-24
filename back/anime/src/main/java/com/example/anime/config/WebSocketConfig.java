package com.example.anime.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.example.anime.handler.CommentWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CommentWebSocketHandler commentWebSocketHandler;

    public WebSocketConfig(CommentWebSocketHandler commentWebSocketHandler) {
        this.commentWebSocketHandler = commentWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(commentWebSocketHandler, "/ws/comments").setAllowedOrigins("*");
    }
}
