package com.marcovavassori.banking.mappers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.dtos.UserDTO;

@Component
public class UserMapper {

    private final AccountMapper accountMapper;

    @Autowired
    public UserMapper(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

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
                        ? user.getAccounts().stream().map(accountMapper::toDTO).toList()
                        : List.of());
    }
}