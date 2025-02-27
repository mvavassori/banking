package com.marcovavassori.banking.services;

import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.marcovavassori.banking.models.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.accessToken}")
    private Long accessTokenExpirationMs;

    @Value("${jwt.expiration.refreshToken}")
    private Long refreshTokenExpirationMs;

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("authorities", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("version", user.getTokenVersion())
                .claim("type", "access")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("authorities", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("version", user.getTokenVersion())
                .claim("type", "refresh")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsernameFromToken(String token) {
        return extractClaimFromToken(token, Claims::getSubject);
    }

    public Date extractExpirationDateFromToken(String token) {
        return extractClaimFromToken(token, Claims::getExpiration);
    }

    public Integer extractVersionFromToken(String token) {
        return extractClaimFromToken(token, claims -> claims.get("version", Integer.class));
    }

    public String extractTypeFromToken(String token) {
        return extractClaimFromToken(token, claims -> claims.get("type", String.class));
    }

    public Long getExpirationTimeInSeconds(String token) {
        Date expirationDate = extractExpirationDateFromToken(token);
        Date now = new Date();
        return (expirationDate.getTime() - now.getTime()) / 1000; // Convert milliseconds to seconds for client
    }

    private boolean isTokenExpired(String token) {
        return extractExpirationDateFromToken(token).before(new Date());
    }

    public boolean validateAccessToken(String token, UserDetails userDetails) {
        User user = (User) userDetails;
        String username = extractUsernameFromToken(token);
        Integer version = extractVersionFromToken(token);
        String type = extractTypeFromToken(token);
        return username.equals(user.getUsername()) && version.equals(user.getTokenVersion()) && type.equals("access")
                && !isTokenExpired(token);
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        User user = (User) userDetails;
        String username = extractUsernameFromToken(token);
        Integer version = extractVersionFromToken(token);
        String type = extractTypeFromToken(token);
        return username.equals(user.getUsername()) && version.equals(user.getTokenVersion()) && type.equals("refresh")
                && !isTokenExpired(token);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}