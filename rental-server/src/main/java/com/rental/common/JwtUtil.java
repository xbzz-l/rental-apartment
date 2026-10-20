package com.rental.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：负责登录令牌的生成与解析
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours}")
    private long expireHours;

    /** 生成 HMAC 签名密钥（HS256，密钥长度需 >= 32 字节） */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成登录令牌
     *
     * @param userId 用户ID，作为 token 的 subject
     * @param role   用户角色（user / admin）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String role) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(key())
                .compact();
    }

    /**
     * 解析并校验令牌
     *
     * @param token JWT 字符串
     * @return 令牌载荷（subject 为用户ID，claim 含 role）
     * @throws io.jsonwebtoken.JwtException 令牌无效或已过期时抛出
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
