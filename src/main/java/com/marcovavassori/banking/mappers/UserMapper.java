package com.marcovavassori.banking.mappers;

import java.util.List;
import org.springframework.stereotype.Component;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.models.Account;
import com.marcovavassori.banking.dtos.UserDTO;
import com.marcovavassori.banking.dtos.AccountDTO;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null)
            return null;

        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getAccounts() != null
                        ? user.getAccounts().stream().map(this::toAccountDTO).toList()
                        : List.of());
    }

    private AccountDTO toAccountDTO(Account account) {
        if (account == null)
            return null;

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