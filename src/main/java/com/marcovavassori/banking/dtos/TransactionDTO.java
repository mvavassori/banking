package com.marcovavassori.banking.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.marcovavassori.banking.models.enums.AccountCurrency;
import com.marcovavassori.banking.models.enums.TransactionType;

public record TransactionDTO(
        Long id,
        String transactionNumber,
        TransactionType transactionType,
        BigDecimal amount,
        String description,
        // Source account information
        Long sourceAccountId,
        String sourceAccountNumber,
        String sourceAccountOwnerName,
        String sourceAccountOwnerSurname,
        // Destination account information
        Long destinationAccountId,
        String destinationAccountNumber,
        String destinationAccountOwnerName,
        String destinationAccountOwnerSurname,
        AccountCurrency currency,
        BigDecimal balanceAfterTransaction,
        String referenceId,
        LocalDateTime createdAt) {
}
