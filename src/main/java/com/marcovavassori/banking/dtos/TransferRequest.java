package com.marcovavassori.banking.dtos;

import java.math.BigDecimal;

public record TransferRequest(
                Long sourceAccountId,
                Long destinationAccountId,
                BigDecimal amount,
                String description) {
}