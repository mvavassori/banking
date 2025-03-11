package com.marcovavassori.banking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.marcovavassori.banking.exceptions.UserNotFoundException;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.repositories.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class) // to enable Mockito support
class UserServiceTest {

    // @Mock is used to create a mock instance of the UserRepository
    @Mock
    private UserRepository userRepository;

    // @InjectMocks tells Mockito to inject the mock repository into the UserService
    // instance
    @InjectMocks
    private UserService userService;

    @Test
    void testGetUser_whenUserExists_returnsUser() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");

        // Mockito.when is used to create a mock behavior for the findById method
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User foundUser = userService.getUser(userId);
        assertNotNull(foundUser);
        assertEquals(userId, foundUser.getId());
        verify(userRepository).findById(userId);
    }

    @Test
    void testGetUser_whenUserDoesNotExist_throwsException() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUser(userId));
        verify(userRepository).findById(userId);
    }

    @Test
    void testGetUserByEmailWithAccounts_whenUserExists_returnsUser() {
        String email = "test@example.com";
        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmailWithAccounts(email)).thenReturn(Optional.of(user));

        User foundUser = userService.getUserByEmailWithAccounts(email);
        assertNotNull(foundUser);
        assertEquals(email, foundUser.getEmail());
        verify(userRepository).findByEmailWithAccounts(email);
    }

    @Test
    void testGetUserByEmailWithAccounts_whenUserDoesNotExist_throwsException() {
        String email = "test@example.com";
        when(userRepository.findByEmailWithAccounts(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByEmailWithAccounts(email));
        verify(userRepository).findByEmailWithAccounts(email);
    }

    @Test
    void testDeleteUser_whenUserExists_deletesUser() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        userService.deleteUser(userId);

        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void testDeleteUser_whenUserDoesNotExist_throwsException() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(userId));
        verify(userRepository).existsById(userId);
        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    void testLoadUserByUsername_whenUserExists_returnsUserDetails() {
        String email = "test@example.com";
        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername(email);
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testLoadUserByUsername_whenUserDoesNotExist_throwsException() {
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.loadUserByUsername(email));
        verify(userRepository).findByEmail(email);
    }
}
