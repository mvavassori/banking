package com.marcovavassori.banking.dtos;

import com.marcovavassori.banking.models.enums.AccountCurrency;
import com.marcovavassori.banking.models.enums.AccountType;

public record CreateAccountRequest(
                AccountType accountType,
                AccountCurrency accountCurrency,
                Long userId // Potentially include userId if admins create accounts for other users
) {
}
