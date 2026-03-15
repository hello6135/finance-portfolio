package com.finance.finportfolio.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Access Token 생성 ──────────────────────────────────────
    public String createAccessToken(String loginId, String role) {
        return Jwts.builder()
                .subject(loginId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Refresh Token 생성 (페이로드 최소화) ───────────────────
    public String createRefreshToken(String loginId) {
        return Jwts.builder()
                .subject(loginId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── 토큰에서 loginId 추출 ──────────────────────────────────
    public String getLoginId(String token) {
        return getClaims(token).getSubject();
    }

    // ── 토큰에서 role 추출 ─────────────────────────────────────
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // ── 토큰 유효성 검증 ───────────────────────────────────────
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw e; // 만료는 별도 처리 (재발급 흐름)
        } catch (JwtException | IllegalArgumentException e) {
            return false; // 위변조, 형식 오류 등
        }
    }

    // ── Access Token 만료 여부만 확인 (재발급 시 사용) ─────────
    public boolean isTokenExpired(String token) {
        try {
            getClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}