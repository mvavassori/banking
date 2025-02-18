package com.marcovavassori.banking.models;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marcovavassori.banking.models.enums.UserRole;

import jakarta.persistence.*; // JPA annotations

@Entity // Marks User as a JPA (Java Persistance Api) entity
@Table(name = "users") // Explicitly name the database table 'users'
public class User {

    @Id // Marks 'id' as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment strategy for MySQL
    private Long id; // Using Long for IDs in JPA entities

    @Column(nullable = false) // Column is required (NOT NULL in DB)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, unique = true) // Column is required and unique
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) // Store the enum as a string in the database
    @Column(nullable = false, name = "role")
    private UserRole role;

    @CreationTimestamp // Automatically sets the timestamp when the entity is created
    @Column(name = "created_at") // Explicitly name the column 'created_at'
    private LocalDateTime createdAt; // Using LocalDateTime for modern date/time

    @UpdateTimestamp // Automatically updates the timestamp when the entity is updated
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-Many relationship with Account (User can have multiple Accounts)
    // user is the field in the Account entity that maps this relationship
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Account> accounts; // List of Accounts associated with this User

    // ** Default Constructor (no-args constructor) - Required by JPA **
    public User() {

    }

    // ** Constructor with arguments (Optional, for convenience when you need to
    // create new entity instances in your application code) **
    public User(String name, String surname, String email, String password, UserRole role) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // **Getters and Setters for all fields (Essential for JPA to work correctly)**

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Hide the password when serializing to JSON
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Account> getAccounts() { // Getter for the accounts list
        return accounts;
    }

    public void setAccounts(List<Account> accounts) { // Setter for the accounts list
        this.accounts = accounts;
    }

}
