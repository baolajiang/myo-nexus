package com.myo.blog.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JWTUtils {

    // 注意：JJWT 0.10+ 强制要求 HS256 的密钥至少 32 个字符 (256 bits)
    // 帮您补全了长度，确保不会报 WeakKeyException
    private static final String jwtToken = "tangmengxi2693398551!@#$$tangmengxi2693398551!@#$$";

    // 创建 Key 对象
    private static SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtToken.getBytes(StandardCharsets.UTF_8));
    }

    public static String createToken(String userId){
        Map<String,Object> claims = new HashMap<>();
        claims.put("userId",userId);

        // JJWT 0.12.x 写法
        return Jwts.builder()
                .signWith(getKey(), Jwts.SIG.HS256) // 使用 Key 对象签名
                .claims(claims) // 设置自定义载荷
                .issuedAt(new Date()) // 设置签发时间
                .expiration(new Date(System.currentTimeMillis() + 100L * 365 * 24 * 60 * 60 * 1000)) // 100 年的有效时间
                .compact();
    }

    public static Map<String, Object> checkToken(String token){
        try {
            // JJWT 0.12.x 写法
            Claims claims = Jwts.parser()
                    .verifyWith(getKey()) // 验证签名
                    .build()
                    .parseSignedClaims(token)
                    .getPayload(); // 获取载荷
            return claims;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


}