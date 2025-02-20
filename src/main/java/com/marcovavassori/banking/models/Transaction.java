package com.marcovavassori.banking.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.marcovavassori.banking.models.enums.AccountCurrency;
import com.marcovavassori.banking.models.enums.TransactionType;

import jakarta.persistence.*;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, name = "transaction_number")
    private String transactionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "transaction_type")
    private TransactionType transactionType; // DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN, INTEREST_CREDIT,
                                             // FEE_DEDUCTION

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    // Account that money comes from (can be null for deposits)
    @ManyToOne
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    // Account that money goes to (can be null for withdrawals)
    @ManyToOne
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountCurrency currency;

    // Records the resulting balance after transaction
    @Column(nullable = false, name = "balance_after_transaction")
    private BigDecimal balanceAfterTransaction;

    // Reference field for linking related transactions (like the two sides of a
    // transfer)
    @Column(name = "reference_id")
    private String referenceId;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(TransactionType transactionType, BigDecimal amount, String description, Account sourceAccount,
            Account destinationAccount, AccountCurrency currency, BigDecimal balanceAfterTransaction) {
        this.transactionNumber = generateTransactionNumber();
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.currency = currency;
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    // Helper method to generate transaction number
    private String generateTransactionNumber() {
        return "TXN" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(Account sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(Account destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public AccountCurrency getCurrency() {
        return currency;
    }

    public void setCurrency(AccountCurrency currency) {
        this.currency = currency;
    }

    public BigDecimal getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public void setBalanceAfterTransaction(BigDecimal balanceAfterTransaction) {
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
