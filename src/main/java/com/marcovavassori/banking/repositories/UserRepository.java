package com.marcovavassori.banking.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.marcovavassori.banking.models.User;

// Repositories are used to interact with the database. They extend JpaRepository to inherit CRUD methods.

@Repository
public interface UserRepository extends JpaRepository<User, Long> { // inherits CRUD methods from JpaRepository
    // Custom query method to find a User by email
    Optional<User> findByEmail(String email); // Spring Data JPA will implement this method for us

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.accounts WHERE u.email = :email")
    Optional<User> findByEmailWithAccounts(String email); // Optional to handle null values

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.accounts WHERE u.id = :id")
    Optional<User> findByIdWithAccounts(Long id);
}
