package com.marcovavassori.banking.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marcovavassori.banking.exceptions.InvalidRefreshTokenException;
import com.marcovavassori.banking.models.RefreshToken;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.repositories.RefreshTokenRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class RefreshTokenService {

    @Value("${jwt.expiration.refreshToken}")
    private long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    public RefreshToken createRefreshToken(User user, HttpServletRequest request) {

        RefreshToken refreshToken = new RefreshToken();

        // Set the token properties
        refreshToken.setToken(jwtService.generateRefreshToken(user));
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setIpAddress(request.getRemoteAddr());
        refreshToken.setDeviceInfo(request.getHeader("User-Agent"));

        return refreshTokenRepository.save(refreshToken);
    }

    // Verify a refresh token by checking in the refresh_tokens table if it exists,
    // is not revoked, and is not expired
    public RefreshToken verifyRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(refreshToken -> !refreshToken.isRevoked())
                .filter(refreshToken -> refreshToken.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new InvalidRefreshTokenException());
    }

    // Rotate a refresh token Used when a user logs out from a single device
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, HttpServletRequest request) {
        // Verify the token is valid first
        verifyRefreshToken(oldToken.getToken());

        // Get the user from the old token
        User user = oldToken.getUser();

        // Revoke the old token
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Create a new token for the same user
        return createRefreshToken(user, request);
    }

    // Revoke all tokens for a user Used when a user changes password or logs out
    // from all devices
    public void revokeAllUserTokens(User user) {
        List<RefreshToken> userTokens = refreshTokenRepository.findAllTokensByUser(user.getId());

        if (!userTokens.isEmpty()) {
            // Mark all tokens as revoked
            userTokens.forEach(token -> token.setRevoked(true));

            // Save all the updated tokens
            refreshTokenRepository.saveAll(userTokens);
        }
    }

    // Find a refresh token by its token string
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // Clean up expired tokens to prevent database bloat. This method is scheduled
    // to run once a day
    @Scheduled(fixedRate = 86400000) // Run once a day (24 * 60 * 60 * 1000 ms)
    public void cleanupExpiredTokens() {
        List<RefreshToken> expiredTokens = refreshTokenRepository
                .findAllExpiredTokens(LocalDateTime.now());

        if (!expiredTokens.isEmpty()) {
            refreshTokenRepository.deleteAll(expiredTokens);
        }
    }
}