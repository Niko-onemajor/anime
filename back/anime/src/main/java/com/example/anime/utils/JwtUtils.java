package com.example.anime.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {
    @Value("${jwt.secret-key}")
    private String secretKey;
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // 生成访问令牌
    public String generateAccessToken(String username) {
        return generateAccessToken(username, false);
    }
    
    // 生成访问令牌（支持记住我）
    public String generateAccessToken(String username, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        long expiration = rememberMe ? accessTokenExpiration * 7 : accessTokenExpiration; // 记住我时，访问令牌有效期延长到7天
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
    
    // 生成刷新令牌
    public String generateRefreshToken(String username) {
        return generateRefreshToken(username, false);
    }
    
    // 生成刷新令牌（支持记住我）
    public String generateRefreshToken(String username, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        long expiration = rememberMe ? refreshTokenExpiration * 2 : refreshTokenExpiration; // 记住我时，刷新令牌有效期延长到14天
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    // 从令牌中获取用户名
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // 验证令牌
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // 生成令牌（保持向后兼容）
    public String generateToken(String username) {
        return generateAccessToken(username);
    }
}
