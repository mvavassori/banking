package com.marcovavassori.banking.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record UserDTO(
        Long id,
        String name,
        String surname,
        String email,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AccountDTO> accounts) {
}