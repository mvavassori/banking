package com.marcovavassori.banking.dtos;

import java.math.BigDecimal;

public record TransactionRequest(
        Long accountId,
        BigDecimal amount,
        String description) {
}