package com.marcovavassori.banking.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.repositories.UserRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Services are the classes that contain the business logic of the application

@Service
public class UserService {

    // Declare dependencies
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired // Inject dependencies via constructor injection
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ** Business Logic Methods **

    public User createUser(User user) {
        // 1. Validate user input
        // Check for blank fields (name, surname, email, password, role) Using a helper
        // method for general user data validation
        if (!isValidUserData(user)) {
            throw new IllegalArgumentException(
                    "All required fields must be filled: name, surname, email, password, role.");
        }
        if (user.getName().length() < 2 || user.getName().length() > 255) {
            throw new IllegalArgumentException("Name must be between 2 and 255 characters long");
        }
        if (user.getSurname().length() < 2 || user.getSurname().length() > 255) {
            throw new IllegalArgumentException("Surname must be between 2 and 255 characters long");
        }
        // Validate email format usign a helper method
        if (!isValidEmail(user.getEmail())) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        // Validate password strength using a helper method
        if (!isStrongPassword(user.getPassword())) {
            throw new IllegalArgumentException("Password must be strong: at least 8 characters, " +
                    "one uppercase, one lowercase, one number, and one special character.");
        }

        // 2. Check if email is already in use
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // 3. Hash the password before saving
        String rawPassword = user.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword); // Hash it using BCryptPasswordEncoder
        user.setPassword(encodedPassword); // Set the hashed password back to the User object

        // Example of using BCryptPasswordEncoder directly without Inversion of Control
        // No longer injected via constructor by the Spring container
        // PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // String encodedPassword = passwordEncoder.encode(rawPassword);

        // 4. Save the user to the database
        return userRepository.save(user);
    }

    public User getUser(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getUserByEmailWithAccounts(String email) {

        return userRepository.findByEmailWithAccounts(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public void deleteUser(Long id) {

        userRepository.deleteById(id);
    }

    // ** Validation / Helper Methods **

    private boolean isValidUserData(User user) { // Helper to check for blank fields
        return StringUtils.hasText(user.getName()) &&
                StringUtils.hasText(user.getSurname()) &&
                StringUtils.hasText(user.getEmail()) &&
                StringUtils.hasText(user.getPassword()) &&
                user.getRole() != null; // Assuming Role is an enum and cannot be null if required in DB
    }

    private boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) { // Check for null or empty using Spring's StringUtils
            return false; // Email is blank
        }
        // Regular expression to match valid email formats
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private boolean isStrongPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return false; // Password is blank
        }
        // Regex for strong password:
        // - At least 8 characters long
        // - At least one uppercase letter
        // - At least one lowercase letter
        // - At least one digit
        // - At least one special character (non-alphanumeric)
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!¡?¿<>,.]).{8,}$";
        Pattern pattern = Pattern.compile(passwordRegex);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

}
