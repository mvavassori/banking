package com.marcovavassori.banking.services;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.marcovavassori.banking.exceptions.AccountNotFoundException;
import com.marcovavassori.banking.exceptions.UserNotFoundException;
import com.marcovavassori.banking.models.Account;
import com.marcovavassori.banking.models.enums.AccountCurrency;
import com.marcovavassori.banking.models.enums.AccountType;
import com.marcovavassori.banking.repositories.AccountRepository;
import com.marcovavassori.banking.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    // ** Business Logic Methods **

    @Transactional
    public void createAccount(Account account) {
        // 1. Validate user input
        validateAccountData(account);

        // 2. Check if the user exists
        Long userId = account.getUser().getId();
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        // Set the full user entity from the database (helps avoid detached entity
        // issues)
        account.setUser(userOpt.get());

        // 3. Generate a unique account number
        String accountNumber = generateAccountNumber();

        // 4. Check if the generated account number is already in use and if it's
        // already used, it generate a new one until it finds a unique one
        while (accountRepository.findByAccountNumber(accountNumber) != null) {
            accountNumber = generateAccountNumber();
        }

        // 5. Set the generated account number
        account.setAccountNumber(accountNumber);

        // 4. Save the account
        accountRepository.save(account);
    }

    public Account getAccount(Long id) {
        var accountOpt = accountRepository.findById(id);
        if (accountOpt.isEmpty()) {
            throw new AccountNotFoundException(id);
        }
        return accountOpt.get();
    }

    // returns all accounts associated with a given user
    public List<Account> getAccountsByUserId(Long userId) {
        // Check if the user exists
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        return accountRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new AccountNotFoundException(id);
        }
        accountRepository.deleteById(id);
    }

    // ** Validation / Helper Methods **

    private void validateAccountData(Account account) { // Helper to check for blank fields
        if (account == null) {
            throw new IllegalArgumentException("Account data cannot be null.");
        }
        if (account.getAccountType() == null) {
            throw new IllegalArgumentException("Account type must be specified.");
        }

        // Check if the account type is valid based on enum AccountType
        if (!Arrays.asList(AccountType.values()).contains(account.getAccountType())) {
            throw new IllegalArgumentException("Invalid account type: " + account.getAccountType());
        }

        if (account.getBalance() == null) {
            throw new IllegalArgumentException("Balance must be specified.");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) { // Ensure balance is not negative
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        if (account.getCurrency() == null) {
            throw new IllegalArgumentException("Currency must be specified.");
        }

        // Check if the currency is valid based on enum AccountCurrency
        if (!Arrays.asList(AccountCurrency.values()).contains(account.getCurrency())) {
            throw new IllegalArgumentException("Invalid currency: " + account.getCurrency());
        }

        if (account.getUser() == null) {
            throw new IllegalArgumentException("User must be associated with the account.");
        }
        if (account.getUser().getId() == null) { // Ensure User object has an ID
            throw new IllegalArgumentException("User ID must be provided for account creation.");
        }
    }

    private String generateAccountNumber() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }

}
