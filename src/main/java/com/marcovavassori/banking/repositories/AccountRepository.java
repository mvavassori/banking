package com.marcovavassori.banking.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.marcovavassori.banking.models.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Just use the default CRUD methods from JpaRepository for now.
    // You can add custom query method when necessary.
}
