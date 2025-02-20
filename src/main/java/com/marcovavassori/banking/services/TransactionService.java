package com.marcovavassori.banking.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marcovavassori.banking.exceptions.UserNotFoundException;
import com.marcovavassori.banking.exceptions.InsufficientBalanceException;
import com.marcovavassori.banking.exceptions.InvalidTransactionException;
import com.marcovavassori.banking.models.Account;
import com.marcovavassori.banking.models.Transaction;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.models.enums.TransactionType;
import com.marcovavassori.banking.repositories.TransactionRepository;
import com.marcovavassori.banking.repositories.UserRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // ** Business Logic Methods **

    // Filter transactions by user, date range, transaction type, amount range
    public Page<Transaction> getFilteredTransactions(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            TransactionType transactionType,
            BigDecimal minAmount,
            BigDecimal maxAmount, Pageable pageable) {

        // Check if start date is before end date
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        // Check if amount ranges are valid
        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum amount cannot be negative");
        }

        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maximum amount cannot be negative");
        }

        // Check if the user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId)); // Custom exception

        // Get the user's accounts
        List<Account> userAccounts = user.getAccounts();

        // Create the specification; that is a functional interface used to build
        // type-safe, dynamic queries
        Specification<Transaction> spec = (root, query, criteriaBuilder) -> {
            // Matches transactions where the source account is in the user's account list
            Predicate sourceAccountIn = root.get("sourceAccount").in(userAccounts);
            // Matches transactions where the destination account is in the user's account
            // list
            Predicate destinationAccountIn = root.get("destinationAccount").in(userAccounts);
            // Combine the predicates with an OR condition to get transactions where either
            // the source or
            // destination account is in the user's account list
            return criteriaBuilder.or(sourceAccountIn, destinationAccountIn);
        };

        // Date range filter
        if (startDate != null || endDate != null) {
            spec = spec.and((root, query, criteriaBuilder) -> {
                if (startDate != null && endDate != null) {
                    return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
                } else if (startDate != null) {
                    return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
                } else {
                    return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
                }
            });
        }

        // Transaction type filter
        if (transactionType != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("transactionType"),
                    transactionType));
        }

        // Amount filters
        if (minAmount != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("amount"),
                    minAmount));
        }
        if (maxAmount != null) {
            spec = spec.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
        }

        // add more filters if needed

        return transactionRepository.findAll(spec, pageable);
    }

    @Transactional
    public Transaction deposit(Account account, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be positive");
        }

        // Update account balance
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);

        // Create transaction record
        Transaction transaction = new Transaction(
                TransactionType.DEPOSIT,
                amount,
                description,
                null, // no source account for deposits
                account,
                account.getCurrency(),
                newBalance);

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction withdraw(Account account, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be positive");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds for withdrawal");
        }

        // Update account balance
        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);

        // Create transaction record
        Transaction transaction = new Transaction(
                TransactionType.WITHDRAWAL,
                amount,
                description,
                account,
                null, // no destination account for withdrawals
                account.getCurrency(),
                newBalance);

        return transactionRepository.save(transaction);
    }

    @Transactional
    public List<Transaction> transfer(Account sourceAccount, Account destinationAccount, BigDecimal amount,
            String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be positive");
        }

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds for transfer");
        }

        if (sourceAccount.equals(destinationAccount)) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }

        // Generate a common reference ID for linking the two transactions
        String referenceId = UUID.randomUUID().toString();

        // Update source account balance
        BigDecimal sourceNewBalance = sourceAccount.getBalance().subtract(amount);
        sourceAccount.setBalance(sourceNewBalance);

        // Update destination account balance
        BigDecimal destinationNewBalance = destinationAccount.getBalance().add(amount);
        destinationAccount.setBalance(destinationNewBalance);

        // Create outgoing transaction
        Transaction outgoingTransaction = new Transaction(
                TransactionType.TRANSFER_OUT,
                amount,
                description,
                sourceAccount,
                destinationAccount,
                sourceAccount.getCurrency(),
                sourceNewBalance);
        outgoingTransaction.setReferenceId(referenceId);

        // Create incoming transaction
        Transaction incomingTransaction = new Transaction(
                TransactionType.TRANSFER_IN,
                amount,
                description,
                sourceAccount,
                destinationAccount,
                destinationAccount.getCurrency(),
                destinationNewBalance);
        incomingTransaction.setReferenceId(referenceId);

        // Save both transactions
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transactionRepository.save(outgoingTransaction));
        transactions.add(transactionRepository.save(incomingTransaction));

        return transactions;
    }

    @Transactional
    public Transaction applyInterest(Account account, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Interest amount must be positive");
        }

        // Update account balance
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);

        // Create interest transaction
        Transaction transaction = new Transaction(
                TransactionType.INTEREST_CREDIT,
                amount,
                description,
                null,
                account,
                account.getCurrency(),
                newBalance);

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction applyFee(Account account, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Fee amount must be positive");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds to apply fee");
        }

        // Update account balance
        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);

        // Create fee transaction
        Transaction transaction = new Transaction(
                TransactionType.FEE_DEDUCTION,
                amount,
                description,
                account,
                null,
                account.getCurrency(),
                newBalance);

        return transactionRepository.save(transaction);
    }

}
