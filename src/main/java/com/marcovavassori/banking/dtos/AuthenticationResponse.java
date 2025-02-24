package com.marcovavassori.banking.dtos;

public record AuthenticationResponse(
        String token,
        Long userId,
        String email,
        String name,
        String surname,
        String role) {
}
