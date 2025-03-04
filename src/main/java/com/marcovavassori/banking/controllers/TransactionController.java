package com.marcovavassori.banking.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.marcovavassori.banking.dtos.TransactionDTO;
import com.marcovavassori.banking.dtos.TransactionRequest;
import com.marcovavassori.banking.dtos.TransferRequest;
import com.marcovavassori.banking.mappers.TransactionMapper;
import com.marcovavassori.banking.models.Account;
import com.marcovavassori.banking.models.Transaction;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.models.enums.TransactionType;
import com.marcovavassori.banking.services.AccountService;
import com.marcovavassori.banking.services.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, AccountService accountService,
            TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.transactionMapper = transactionMapper;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    private boolean canAccessAccount(Authentication authentication, Account account) {
        User authenticatedUser = (User) authentication.getPrincipal();
        return isAdmin(authentication) || account.getUser().getId().equals(authenticatedUser.getId());
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDTO>> getFilteredTransactions(
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            Pageable pageable,
            Authentication authentication) {

        // Check if user has permission to view these transactions
        User authenticatedUser = (User) authentication.getPrincipal();
        if (!isAdmin(authentication) && !userId.equals(authenticatedUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<Transaction> transactions = transactionService.getFilteredTransactions(
                userId, startDate, endDate, transactionType, minAmount, maxAmount, pageable);

        Page<TransactionDTO> transactionDTOs = transactions.map(transactionMapper::toDTO);

        return ResponseEntity.ok(transactionDTOs);
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionDTO> deposit(
            @RequestBody TransactionRequest request,
            Authentication authentication) {

        // Validate request
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        if (request.accountId() == null) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Deposit request: accountId={}, amount={}",
                request.accountId(), request.amount());

        Account account = accountService.getAccount(request.accountId());
        if (!canAccessAccount(authentication, account)) {
            logger.warn("Unauthorized deposit attempt to account {} by user {}",
                    request.accountId(), authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Transaction transaction = transactionService.deposit(
                account,
                request.amount(),
                request.description());

        logger.info("Deposit successful: transactionId={}, accountId={}, amount={}",
                transaction.getId(), account.getId(), request.amount());

        return ResponseEntity.ok(transactionMapper.toDTO(transaction));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionDTO> withdraw(
            @RequestBody TransactionRequest request,
            Authentication authentication) {

        // Validate request
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        if (request.accountId() == null) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Withdraw request: accountId={}, amount={}",
                request.accountId(), request.amount());

        Account account = accountService.getAccount(request.accountId());
        if (!canAccessAccount(authentication, account)) {
            logger.warn("Unauthorized withdraw attempt to account {} by user {}",
                    request.accountId(), authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Transaction transaction = transactionService.withdraw(
                account,
                request.amount(),
                request.description());

        logger.info("Withdraw successful: transactionId={}, accountId={}, amount={}",
                transaction.getId(), account.getId(), request.amount());

        return ResponseEntity.ok(transactionMapper.toDTO(transaction));
    }

    @PostMapping("/transfer")
    public ResponseEntity<List<TransactionDTO>> transfer(
            @RequestBody TransferRequest request,
            Authentication authentication) {

        // Validate request
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        if (request.sourceAccountId() == null || request.destinationAccountId() == null) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Transfer request: sourceAccountId={}, destinationAccountId={}, amount={}",
                request.sourceAccountId(), request.destinationAccountId(), request.amount());

        // Get both accounts involved in the transfer
        Account sourceAccount = accountService.getAccount(request.sourceAccountId());
        Account destinationAccount = accountService.getAccount(request.destinationAccountId());

        // Security check: verify the authenticated user owns the source account
        if (!canAccessAccount(authentication, sourceAccount)) {
            logger.warn("Unauthorized transfer attempt from account {} to account {} by user {}",
                    request.sourceAccountId(), request.destinationAccountId(), authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Perform the transfer - this creates TWO transactions:
        // 1. A TRANSFER_OUT transaction for the source account
        // 2. A TRANSFER_IN transaction for the destination account
        List<Transaction> transactions = transactionService.transfer(
                sourceAccount,
                destinationAccount,
                request.amount(),
                request.description());

        // Convert the list of Transaction entities to DTOs
        List<TransactionDTO> transactionDTOs = transactions.stream()
                .map(transactionMapper::toDTO)
                .toList();

        // Log the successful transfer
        logger.info("Transfer successful: sourceAccountId={}, destinationAccountId={}, amount={}, transactionIds={}",
                sourceAccount.getId(), destinationAccount.getId(), request.amount(),
                transactions.stream().map(t -> t.getId().toString()).reduce((a, b) -> a + "," + b).orElse(""));

        return ResponseEntity.ok(transactionDTOs);
    }

    @PostMapping("/interest")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TransactionDTO> applyInterest(
            @RequestBody TransactionRequest request,
            Authentication authentication) {

        // Validate request
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        if (request.accountId() == null) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Apply interest request: accountId={}, amount={}",
                request.accountId(), request.amount());

        Account account = accountService.getAccount(request.accountId());
        Transaction transaction = transactionService.applyInterest(
                account,
                request.amount(),
                request.description());

        logger.info("Interest applied: transactionId={}, accountId={}, amount={}",
                transaction.getId(), account.getId(), request.amount());

        return ResponseEntity.ok(transactionMapper.toDTO(transaction));
    }

    @PostMapping("/fee")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TransactionDTO> applyFee(
            @RequestBody TransactionRequest request,
            Authentication authentication) {

        // Validate request
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        if (request.accountId() == null) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Apply fee request: accountId={}, amount={}",
                request.accountId(), request.amount());

        Account account = accountService.getAccount(request.accountId());
        Transaction transaction = transactionService.applyFee(
                account,
                request.amount(),
                request.description());

        logger.info("Fee applied: transactionId={}, accountId={}, amount={}",
                transaction.getId(), account.getId(), request.amount());

        return ResponseEntity.ok(transactionMapper.toDTO(transaction));
    }
}