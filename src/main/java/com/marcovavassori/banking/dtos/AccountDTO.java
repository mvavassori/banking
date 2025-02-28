package com.marcovavassori.banking.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountDTO(
        Long id,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}