package com.marcovavassori.banking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.marcovavassori.banking.exceptions.InsufficientBalanceException;
import com.marcovavassori.banking.exceptions.InvalidTransactionException;
import com.marcovavassori.banking.exceptions.UserNotFoundException;
import com.marcovavassori.banking.models.Account;
import com.marcovavassori.banking.models.Transaction;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.models.enums.AccountCurrency;
import com.marcovavassori.banking.models.enums.TransactionType;
import com.marcovavassori.banking.repositories.TransactionRepository;
import com.marcovavassori.banking.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    // ----- getFilteredTransactions Tests -----

    @SuppressWarnings("unchecked")
    @Test
    void testGetFilteredTransactions_success() {
        Long userId = 1L;
        // Create a user with one account (minimal setup)
        Account account = new Account();
        account.setId(10L);
        User user = new User();
        user.setId(userId);
        user.setAccounts(List.of(account)); // Assuming a setter exists

        // Filter parameters
        LocalDateTime startDate = LocalDateTime.now().minusDays(5);
        LocalDateTime endDate = LocalDateTime.now();
        TransactionType type = TransactionType.DEPOSIT;
        BigDecimal minAmount = BigDecimal.ZERO;
        BigDecimal maxAmount = new BigDecimal("1000");
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Create a dummy transaction to return in a page
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        List<Transaction> transactions = Collections.singletonList(transaction);
        Page<Transaction> page = new PageImpl<>(transactions, pageable, transactions.size());
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Transaction> result = transactionService.getFilteredTransactions(
                userId, startDate, endDate, type, minAmount, maxAmount, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findById(userId);
        verify(transactionRepository).findAll((Specification<Transaction>) any(Specification.class), eq(pageable));

    }

    @Test
    void testGetFilteredTransactions_invalidDateRange_throwsException() {
        Long userId = 1L;
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().minusDays(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.getFilteredTransactions(userId, startDate, endDate,
                    null, null, null, PageRequest.of(0, 10));
        });
        assertEquals("Start date must be before or equal to end date", exception.getMessage());
    }

    @Test
    void testGetFilteredTransactions_negativeMinAmount_throwsException() {
        Long userId = 1L;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.getFilteredTransactions(userId, null, null,
                    null, new BigDecimal("-1"), null, PageRequest.of(0, 10));
        });
        assertEquals("Minimum amount cannot be negative", exception.getMessage());
    }

    @Test
    void testGetFilteredTransactions_negativeMaxAmount_throwsException() {
        Long userId = 1L;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.getFilteredTransactions(userId, null, null,
                    null, null, new BigDecimal("-1"), PageRequest.of(0, 10));
        });
        assertEquals("Maximum amount cannot be negative", exception.getMessage());
    }

    @Test
    void testGetFilteredTransactions_userNotFound_throwsException() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            transactionService.getFilteredTransactions(userId, null, null,
                    null, null, null, PageRequest.of(0, 10));
        });
        verify(userRepository).findById(userId);
    }

    // ----- deposit Tests -----

    @Test
    void testDeposit_success() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(AccountCurrency.USD);

        BigDecimal depositAmount = new BigDecimal("100.00");
        String description = "Deposit test";
        BigDecimal newBalance = account.getBalance().add(depositAmount);

        // Create a transaction as it would be constructed by the service
        Transaction transaction = new Transaction(
                TransactionType.DEPOSIT,
                depositAmount,
                description,
                null,
                account,
                account.getCurrency(),
                newBalance);
        transaction.setId(1L);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionService.deposit(account, depositAmount, description);

        assertNotNull(result);
        assertEquals(TransactionType.DEPOSIT, result.getTransactionType());
        assertEquals(depositAmount, result.getAmount());
        // Check that the account's balance is updated correctly
        assertEquals(newBalance, account.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testDeposit_invalidAmount_throwsException() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(AccountCurrency.USD);

        BigDecimal invalidAmount = BigDecimal.ZERO;
        String description = "Invalid deposit";

        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.deposit(account, invalidAmount, description);
        });
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ----- withdraw Tests -----

    @Test
    void testWithdraw_success() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("200.00"));
        account.setCurrency(AccountCurrency.USD);

        BigDecimal withdrawAmount = new BigDecimal("50.00");
        String description = "Withdraw test";
        BigDecimal newBalance = account.getBalance().subtract(withdrawAmount);

        Transaction transaction = new Transaction(
                TransactionType.WITHDRAWAL,
                withdrawAmount,
                description,
                account,
                null,
                account.getCurrency(),
                newBalance);
        transaction.setId(1L);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionService.withdraw(account, withdrawAmount, description);

        assertNotNull(result);
        assertEquals(TransactionType.WITHDRAWAL, result.getTransactionType());
        assertEquals(newBalance, account.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testWithdraw_invalidAmount_throwsException() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("200.00"));
        account.setCurrency(AccountCurrency.USD);

        BigDecimal invalidAmount = BigDecimal.ZERO;
        String description = "Invalid withdrawal";

        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.withdraw(account, invalidAmount, description);
        });
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testWithdraw_insufficientBalance_throwsException() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("50.00"));
        account.setCurrency(AccountCurrency.USD);

        BigDecimal withdrawAmount = new BigDecimal("100.00");
        String description = "Insufficient funds";

        assertThrows(InsufficientBalanceException.class, () -> {
            transactionService.withdraw(account, withdrawAmount, description);
        });
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ----- transfer Tests -----

    @Test
    void testTransfer_success() {
        Account sourceAccount = new Account();
        sourceAccount.setId(1L);
        sourceAccount.setBalance(new BigDecimal("500.00"));
        sourceAccount.setCurrency(AccountCurrency.USD);

        Account destinationAccount = new Account();
        destinationAccount.setId(2L);
        destinationAccount.setBalance(new BigDecimal("200.00"));
        destinationAccount.setCurrency(AccountCurrency.USD);

        BigDecimal transferAmount = new BigDecimal("100.00");
        String description = "Transfer test";

        // Simulate saving by returning the same transaction with an id assigned.
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        List<Transaction> result = transactionService.transfer(sourceAccount, destinationAccount, transferAmount,
                description);

        assertNotNull(result);
        assertEquals(2, result.size());
        // Verify that source and destination balances have been updated
        assertEquals(new BigDecimal("400.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("300.00"), destinationAccount.getBalance());
        // Verify that both transactions share the same reference id
        String referenceId = result.get(0).getReferenceId();
        assertNotNull(referenceId);
        assertEquals(referenceId, result.get(1).getReferenceId());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void testTransfer_invalidAmount_throwsException() {
        Account sourceAccount = new Account();
        sourceAccount.setId(1L);
        sourceAccount.setBalance(new BigDecimal("500.00"));
        sourceAccount.setCurrency(AccountCurrency.USD);

        Account destinationAccount = new Account();
        destinationAccount.setId(2L);
        destinationAccount.setBalance(new BigDecimal("200.00"));
        destinationAccount.setCurrency(AccountCurrency.USD);

        BigDecimal invalidAmount = BigDecimal.ZERO;
        String description = "Invalid transfer";

        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.transfer(sourceAccount, destinationAccount, invalidAmount, description);
        });
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testTransfer_insufficientBalance_throwsException() {
        Account sourceAccount = new Account();
        sourceAccount.setId(1L);
        sourceAccount.setBalance(new BigDecimal("50.00"));
        sourceAccount.setCurrency(AccountCurrency.USD);

        Account destinationAccount = new Account();
        destinationAccount.setId(2L);
        destinationAccount.setBalance(new BigDecimal("200.00"));
        destinationAccount.setCurrency(AccountCurrency.USD);

        BigDecimal transferAmount = new BigDecimal("100.00");
        String description = "Insufficient funds transfer";

        assertThrows(InsufficientBalanceException.class, () -> {
            transactionService.transfer(sourceAccount, destinationAccount, transferAmount, description);
        });
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testTransfer_sameAccount_throwsException() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("500.00"));
        account.setCurrency(AccountCurrency.USD);

        BigDecimal transferAmount = new BigDecimal("100.00");
        String description = "Same account transfer";

        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.transfer(account, account, transferAmount, description);
        });
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ----- applyInterest Tests -----

    @Test
    void testApplyInterest_success() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency(AccountCurrency.USD);

        BigDecimal interestAmount = new BigDecimal("50.00");
        String description = "Interest credit";
        BigDecimal newBalance = account.getBalance().add(interestAmount);

        Transaction transaction = new Transaction(
                TransactionType.INTEREST_CREDIT,
                interestAmount,
                description,
                null,
                account,
                account.getCurrency(),
                newBalance);
        transaction.setId(1L);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionService.applyInterest(account, interestAmount, description);

        assertNotNull(result);
        assertEquals(TransactionType.INTEREST_CREDIT, result.getTransactionType());
        assertEquals(newBalance, account.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testApplyInterest_invalidAmount_throwsException() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency(AccountCurrency.USD);

        BigDecimal invalidAmount = BigDecimal.ZERO;
        String description = "Invalid interest";

        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.applyInterest(account, invalidAmount, description);
        });
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ----- applyFee Tests -----

    @Test
    void testApplyFee_success() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency(AccountCurrency.USD);

        BigDecimal feeAmount = new BigDecimal("50.00");
        String description = "Fee deduction";
        BigDecimal newBalance = account.getBalance().subtract(feeAmount);

        Transaction transaction = new Transaction(
                TransactionType.FEE_DEDUCTION,
                feeAmount,
                description,
                account,
                null,
                account.getCurrency(),
                newBalance);
        transaction.setId(1L);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionService.applyFee(account, feeAmount, description);

        assertNotNull(result);
        assertEquals(TransactionType.FEE_DEDUCTION, result.getTransactionType());
        assertEquals(newBalance, account.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testApplyFee_invalidAmount_throwsException() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency(AccountCurrency.USD);

        BigDecimal invalidAmount = BigDecimal.ZERO;
        String description = "Invalid fee";

        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.applyFee(account, invalidAmount, description);
        });
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testApplyFee_insufficientBalance_throwsException() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("40.00"));
        account.setCurrency(AccountCurrency.USD);

        BigDecimal feeAmount = new BigDecimal("50.00");
        String description = "Insufficient funds fee";

        assertThrows(InsufficientBalanceException.class, () -> {
            transactionService.applyFee(account, feeAmount, description);
        });
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
