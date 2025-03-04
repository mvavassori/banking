package com.marcovavassori.banking.controllers;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marcovavassori.banking.dtos.AccountDTO;
import com.marcovavassori.banking.dtos.CreateAccountRequest;
import com.marcovavassori.banking.mappers.AccountMapper;
import com.marcovavassori.banking.models.Account;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.models.enums.AccountCurrency;
import com.marcovavassori.banking.models.enums.AccountType;
import com.marcovavassori.banking.services.AccountService;
import com.marcovavassori.banking.services.UserService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    @Autowired
    public AccountController(AccountService accountService, AccountMapper accountMapper, UserService userService) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    @PostMapping
    public ResponseEntity<String> createAccount(@RequestBody CreateAccountRequest createAccountRequest,
            Authentication authentication) {

        if (createAccountRequest.accountType() == null || createAccountRequest.accountCurrency() == null) {
            return ResponseEntity.badRequest().body("Account type and currency are required");
        }

        if (Stream.of(AccountType.values()).anyMatch(v -> v.name().equals(createAccountRequest.accountType()))) {
            return ResponseEntity.badRequest().body("Invalid account type");
        }

        if (Stream.of(AccountCurrency.values())
                .anyMatch(v -> v.name().equals(createAccountRequest.accountCurrency()))) {
            return ResponseEntity.badRequest().body("Invalid account currency");
        }

        // The authenticated user can only create accounts for themselves
        User authenticatedUser = (User) authentication.getPrincipal();
        Long ownerId = authenticatedUser.getId();

        logger.info("createAccount called with ownerId: {}", ownerId);

        // If the authenticated user is an admin, they might be creating an account for
        // another user.
        if (isAdmin(authentication) && createAccountRequest.userId() != null) {
            ownerId = createAccountRequest.userId();
        } else if (!isAdmin(authentication) && createAccountRequest.userId() != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Regular user cant create account for another
                                                                        // user.
        }
        accountService.createAccount(createAccountRequest, ownerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccount(@PathVariable Long id, Authentication authentication) {
        Account account = accountService.getAccount(id);

        // Check if the authenticated user owns this account and allow admins to view
        // any account
        User authenticatedUser = (User) authentication.getPrincipal();
        if (!isAdmin(authentication) && !account.getUser().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(accountMapper.toDTO(account));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountDTO>> getAccountsByUserId(@PathVariable Long userId,
            Authentication authentication) {
        // Users can only view their own accounts and admins can view any user's
        // accounts
        User authenticatedUser = (User) authentication.getPrincipal();
        if (!isAdmin(authentication) && !userId.equals(authenticatedUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Account> accounts = accountService.getAccountsByUserId(userId);
        List<AccountDTO> accountDTOs = accounts.stream().map(accountMapper::toDTO).toList();
        return ResponseEntity.ok(accountDTOs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id, Authentication authentication) {
        // First get the account to check ownership
        Account account = accountService.getAccount(id);

        // Check if the authenticated user owns this account and allow admins to delete
        // any account
        User authenticatedUser = (User) authentication.getPrincipal();
        if (!isAdmin(authentication) && !account.getUser().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
