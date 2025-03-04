package com.marcovavassori.banking.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.marcovavassori.banking.dtos.CreateAccountRequest;
import com.marcovavassori.banking.exceptions.AccountNotFoundException;
import com.marcovavassori.banking.exceptions.UserNotFoundException;
import com.marcovavassori.banking.models.Account;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.repositories.AccountRepository;
import com.marcovavassori.banking.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    // ** Business Logic Methods **

    @Transactional
    public void createAccount(CreateAccountRequest createAccountRequest, Long userId) {
        // Check if the user exists
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("Failed to create account: User with ID {} not found", userId);
            throw new UserNotFoundException(userId);
        }

        User user = userOpt.get();
        // Set the full user entity from the database (helps avoid detached entity
        // issues)
        Account account = new Account();

        account.setAccountType(createAccountRequest.accountType());
        account.setCurrency(createAccountRequest.accountCurrency());
        account.setUser(user);
        account.setBalance(BigDecimal.ZERO); // Set the initial balance to zero

        // Generate a unique account number
        String accountNumber = generateAccountNumber();

        // Check if the generated account number is already in use and if it's
        // already used, it generate a new one until it finds a unique one
        while (accountRepository.findByAccountNumber(accountNumber) != null) {
            accountNumber = generateAccountNumber();
        }

        // Set the generated account number
        account.setAccountNumber(accountNumber);

        // Save the account
        accountRepository.save(account);
        logger.info("Created new account: number={}, type={}, userId={}",
                accountNumber, account.getAccountType(), userId);
    }

    public Account getAccount(Long id) {
        var accountOpt = accountRepository.findById(id);
        if (accountOpt.isEmpty()) {
            logger.warn("Failed to get account: Account not found with ID: {}", id);
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
            logger.warn("Failed to delete account: Account not found with ID: {}", id);
            throw new AccountNotFoundException(id);
        }
        accountRepository.deleteById(id);
        logger.info("Successfully deleted account with ID: {}", id);
    }

    // ** Helper Methods **

    private String generateAccountNumber() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }

}
