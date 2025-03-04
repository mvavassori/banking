package com.marcovavassori.banking.mappers;

import org.springframework.stereotype.Component;
import com.marcovavassori.banking.models.Transaction;
import com.marcovavassori.banking.dtos.TransactionDTO;

@Component
public class TransactionMapper {

    public TransactionDTO toDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return new TransactionDTO(
                transaction.getId(),
                transaction.getTransactionNumber(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getDescription(),
                // Source account details
                transaction.getSourceAccount() != null ? transaction.getSourceAccount().getId() : null,
                transaction.getSourceAccount() != null ? transaction.getSourceAccount().getAccountNumber() : null,
                transaction.getSourceAccount() != null ? transaction.getSourceAccount().getUser().getName() : null,
                transaction.getSourceAccount() != null ? transaction.getSourceAccount().getUser().getSurname() : null,
                // Destination account details
                transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getId() : null,
                transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getAccountNumber() : null,
                transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getUser().getName() : null,
                transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getUser().getSurname() : null,
                transaction.getCurrency(),
                transaction.getBalanceAfterTransaction(),
                transaction.getReferenceId(),
                transaction.getCreatedAt());
    }
}