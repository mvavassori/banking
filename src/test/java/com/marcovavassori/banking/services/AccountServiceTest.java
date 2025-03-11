package com.marcovavassori.banking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marcovavassori.banking.dtos.CreateAccountRequest;
import com.marcovavassori.banking.exceptions.AccountNotFoundException;
import com.marcovavassori.banking.exceptions.UserNotFoundException;
import com.marcovavassori.banking.models.Account;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.models.enums.AccountCurrency;
import com.marcovavassori.banking.models.enums.AccountType;
import com.marcovavassori.banking.repositories.AccountRepository;
import com.marcovavassori.banking.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void testCreateAccount_success() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        CreateAccountRequest request = new CreateAccountRequest(AccountType.CHECKING, AccountCurrency.USD, userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(null);

        accountService.createAccount(request, userId);

        verify(userRepository).findById(userId);
        verify(accountRepository).findByAccountNumber(anyString());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testCreateAccount_userNotFound_throwsException() {
        Long userId = 1L;
        CreateAccountRequest request = new CreateAccountRequest(AccountType.CHECKING, AccountCurrency.USD, userId);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> accountService.createAccount(request, userId));

        verify(userRepository).findById(userId);
        verify(accountRepository, never()).findByAccountNumber(anyString());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void testGetAccount_success() {
        Long accountId = 100L;
        Account account = new Account();
        account.setId(accountId);
        // You can set other minimal fields as needed (e.g., balance, accountNumber)

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Account foundAccount = accountService.getAccount(accountId);

        assertNotNull(foundAccount);
        assertEquals(accountId, foundAccount.getId());
        verify(accountRepository).findById(accountId);
    }

    @Test
    void testGetAccount_notFound_throwsException() {
        Long accountId = 100L;

        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(accountId));
        verify(accountRepository).findById(accountId);
    }

    // ----- Tests for getAccountsByUserId -----

    @Test
    void testGetAccountsByUserId_success() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        Account account1 = new Account();
        account1.setId(101L);
        Account account2 = new Account();
        account2.setId(102L);
        List<Account> accounts = List.of(account1, account2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByUserId(userId)).thenReturn(accounts);

        List<Account> result = accountService.getAccountsByUserId(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findById(userId);
        verify(accountRepository).findByUserId(userId);
    }

    @Test
    void testGetAccountsByUserId_userNotFound_throwsException() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> accountService.getAccountsByUserId(userId));
        verify(userRepository).findById(userId);
        verify(accountRepository, never()).findByUserId(anyLong());
    }

    // ----- Tests for deleteAccount -----

    @Test
    void testDeleteAccount_success() {
        Long accountId = 200L;

        when(accountRepository.existsById(accountId)).thenReturn(true);

        accountService.deleteAccount(accountId);

        verify(accountRepository).existsById(accountId);
        verify(accountRepository).deleteById(accountId);
    }

    @Test
    void testDeleteAccount_notFound_throwsException() {
        Long accountId = 200L;

        when(accountRepository.existsById(accountId)).thenReturn(false);

        assertThrows(AccountNotFoundException.class, () -> accountService.deleteAccount(accountId));
        verify(accountRepository).existsById(accountId);
        verify(accountRepository, never()).deleteById(anyLong());
    }

}
