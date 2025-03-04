package com.marcovavassori.banking.mappers;

import org.springframework.stereotype.Component;

import com.marcovavassori.banking.dtos.AccountDTO;
import com.marcovavassori.banking.models.Account;

@Component
public class AccountMapper {

    public AccountDTO toDTO(Account account) {
        if (account == null) {
            return null;
        }

        return new AccountDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getBalance(),
                account.getCurrency().name(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}