package com.marcovavassori.banking.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.marcovavassori.banking.exceptions.UserNotFoundException;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.repositories.UserRepository;

@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    // Declare dependencies
    private final UserRepository userRepository;

    @Autowired // Inject dependencies via constructor injection
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ** Business Logic Methods **

    public User getUser(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User getUserByEmailWithAccounts(String email) {

        return userRepository.findByEmailWithAccounts(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            logger.warn("Failed to delete user: User not found with ID: {}", id);
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    // This method is required by the UserDetailsService interface
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(email) // we are using the email as the username
                .orElseThrow(() -> new UserNotFoundException(email));
    }

}
