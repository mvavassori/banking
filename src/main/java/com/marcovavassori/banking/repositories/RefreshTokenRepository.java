package com.marcovavassori.banking.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.marcovavassori.banking.models.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Query("SELECT rt FROM RefreshToken rt INNER JOIN rt.user u WHERE u.id = :userId AND rt.revoked = false")
    List<RefreshToken> findAllTokensByUser(Long userId);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiryDate < :now")
    List<RefreshToken> findAllExpiredTokens(LocalDateTime now);
}
