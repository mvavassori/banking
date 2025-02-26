package com.marcovavassori.banking.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.marcovavassori.banking.dtos.AuthenticationResponse;
import com.marcovavassori.banking.dtos.SignInRequest;
import com.marcovavassori.banking.dtos.SignUpRequest;
import com.marcovavassori.banking.exceptions.EmailAlreadyExistsException;
import com.marcovavassori.banking.exceptions.InvalidRequestException;
import com.marcovavassori.banking.exceptions.UserNotFoundException;
import com.marcovavassori.banking.exceptions.ValidationException;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.models.enums.UserRole;
import com.marcovavassori.banking.repositories.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthenticationResponse signUp(SignUpRequest request) {
        // 1. Validate request data
        validateSignUpRequest(request);

        // 2. Check if email is already in use
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException();
        }

        // 3. Create User entity from request
        User user = new User();
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.valueOf(request.role().toUpperCase()));

        try {
            // 4. Save the user to the database
            User savedUser = userRepository.save(user);

            // 5. Generate the accessToken and refreshToken
            String accessToken = jwtService.generateAccessToken(user.getEmail());
            String refreshToken = jwtService.generateRefreshToken(user.getEmail());

            // 6. Get the expiration times for the tokens
            Long accessTokenExpiration = jwtService.getExpirationTimeInSeconds(accessToken);
            Long refreshTokenExpiration = jwtService.getExpirationTimeInSeconds(refreshToken);

            // 7. Return the response
            return new AuthenticationResponse(
                    accessToken,
                    refreshToken,
                    accessTokenExpiration,
                    refreshTokenExpiration,
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getName(),
                    savedUser.getSurname(),
                    savedUser.getRole().name());
        } catch (Exception e) {
            // throw generic exception
            throw new RuntimeException("An unexpected error occurred");
        }

    }

    public AuthenticationResponse signIn(SignInRequest request) {
        try {
            // Use AuthenticationManager to validate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()));

            // If authentication was successful, get the user
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new UserNotFoundException(request.email()));

            // Generate the accessToken and refreshToken
            String accessToken = jwtService.generateAccessToken(user.getEmail());
            String refreshToken = jwtService.generateRefreshToken(user.getEmail());

            // Get the expiration times for the tokens
            Long accessTokenExpiration = jwtService.getExpirationTimeInSeconds(accessToken);
            Long refreshTokenExpiration = jwtService.getExpirationTimeInSeconds(refreshToken);

            // Return the response
            return new AuthenticationResponse(
                    accessToken,
                    refreshToken,
                    accessTokenExpiration,
                    refreshTokenExpiration,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getSurname(),
                    user.getRole().name());
        } catch (AuthenticationException e) {
            throw new InvalidRequestException("Invalid email or password");
        }
    }

    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request,
            HttpServletResponse response) {

        // Get the authorization header from the request
        String authorizationHeader = request.getHeader("Authorization");
        // Check if the authrizationHeader is valid
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return new ResponseEntity<AuthenticationResponse>(HttpStatus.UNAUTHORIZED);
        }

        // Extract the refreshToken from the authorization header
        String refreshToken = authorizationHeader.substring(7);

        // Extract the username from the refreshToken
        String username = jwtService.extractUsernameFromToken(refreshToken);

        // Check if the user exists in the database
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // Check if the refreshToken is valid
        if (jwtService.validateRefreshToken(refreshToken, user)) {
            // Generate a new accessToken
            String accessToken = jwtService.generateAccessToken(user.getEmail());

            // Get the expiration time for the accessToken
            Long accessTokenExpiration = jwtService.getExpirationTimeInSeconds(accessToken);

            Long refreshTokenExpiration = jwtService.getExpirationTimeInSeconds(refreshToken);

            // Return the response
            return ResponseEntity.ok(new AuthenticationResponse(
                    accessToken,
                    refreshToken,
                    accessTokenExpiration,
                    refreshTokenExpiration,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getSurname(),
                    user.getRole().name()));
        } else {
            return new ResponseEntity<AuthenticationResponse>(HttpStatus.UNAUTHORIZED);
        }

    }

    // ** Validation / Helper Methods **

    private void validateSignUpRequest(SignUpRequest request) {
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (!StringUtils.hasText(request.name())) {
            errors.add("Name is required");
        }
        if (!StringUtils.hasText(request.surname())) {
            errors.add("Surname is required");
        }
        if (!StringUtils.hasText(request.email())) {
            errors.add("Email is required");
        }
        if (!StringUtils.hasText(request.password())) {
            errors.add("Password is required");
        }
        if (!StringUtils.hasText(request.role())) {
            errors.add("Role is required");
        }
        // Validate name length
        if (StringUtils.hasText(request.name()) &&
                (request.name().length() < 2 || request.name().length() > 255)) {
            errors.add("Name must be between 2 and 255 characters long");
        }

        // Validate surname length
        if (StringUtils.hasText(request.surname()) &&
                (request.surname().length() < 2 || request.surname().length() > 255)) {
            errors.add("Surname must be between 2 and 255 characters long");
        }

        // Validate email format
        if (StringUtils.hasText(request.email()) && !isValidEmail(request.email())) {
            errors.add("Invalid email format");
        }

        // Validate password strength
        if (StringUtils.hasText(request.password()) && !isStrongPassword(request.password())) {
            errors.add(
                    "Password must be strong: at least 8 characters, one uppercase, one lowercase, one number, and one special character");
        }

        // Validate role
        if (StringUtils.hasText(request.role())) {
            try {
                UserRole.valueOf(request.role().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid role. Must be either USER or ADMIN");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
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
