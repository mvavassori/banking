package com.marcovavassori.banking.dtos;

public record AuthenticationResponse(
                String accessToken,
                String refreshToken,
                Long accessTokenExpiresIn, // Duration in seconds until token expires
                Long refreshTokenExpiresIn, // Duration in seconds until refresh token expires
                Long id,
                String email,
                String name,
                String surname,
                String role) {
}
