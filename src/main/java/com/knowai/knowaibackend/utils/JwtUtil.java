package com.knowai.knowaibackend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@SuppressWarnings("all")
public class JwtUtil {

    // 密钥
    private static final String SECRET = "knowai_abcdefghijklmnopqrstuvwxyz";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    // 24小时有效期
    private static final long EXPIRE = 1000 * 60 * 60 * 24;

    /**
     * 根据用户id创建JWT token
     * @param userId
     * @return jwt字符串token 过期时间1000*60*60*24
     */
    public static String createToken(Long userId){

        return Jwts.builder().setSubject(userId.toString()).
                setExpiration(new Date(System.currentTimeMillis()+EXPIRE)).
                signWith(KEY).compact();
    }

    /**
     * 根据token解析userId
     * @param token
     * @return
     */
    public static Long getUserId(String token){
        Claims claims = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
        return Long.valueOf(claims.getSubject());
    }

}
