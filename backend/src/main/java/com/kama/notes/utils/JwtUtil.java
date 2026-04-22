package com.kama.notes.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private static final long REFRESH_THRESHOLD_SECONDS = 3 * 24 * 60 * 60;

    /**
     * 生成JWT令牌
     */
    public String generateToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 从token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            return Long.valueOf(claims.get("userId").toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Date getExpirationFromToken(String token) {
        try {
            return getClaims(token).getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public long getRemainingSeconds(String token) {
        Date expirationDate = getExpirationFromToken(token);
        if (expirationDate == null) {
            return 0;
        }
        long remainingMillis = expirationDate.getTime() - System.currentTimeMillis();
        return Math.max(0, remainingMillis / 1000);
    }

    public boolean shouldRefreshToken(String token) {
        long remainingSeconds = getRemainingSeconds(token);
        return remainingSeconds > 0 && remainingSeconds <= REFRESH_THRESHOLD_SECONDS;
    }

    /**
     * 刷新JWT令牌
     */
    public String refreshToken(Long userId) {
        return generateToken(userId);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
} 