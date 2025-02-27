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
import com.marcovavassori.banking.dtos.ChangePasswordRequest;
import com.marcovavassori.banking.dtos.SignInRequest;
import com.marcovavassori.banking.dtos.SignUpRequest;
import com.marcovavassori.banking.exceptions.EmailAlreadyExistsException;
import com.marcovavassori.banking.exceptions.InvalidRefreshTokenException;
import com.marcovavassori.banking.exceptions.InvalidRequestException;
import com.marcovavassori.banking.exceptions.UserNotFoundException;
import com.marcovavassori.banking.exceptions.ValidationException;
import com.marcovavassori.banking.models.RefreshToken;
import com.marcovavassori.banking.models.User;
import com.marcovavassori.banking.models.enums.UserRole;
import com.marcovavassori.banking.repositories.RefreshTokenRepository;
import com.marcovavassori.banking.repositories.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public AuthenticationResponse signUp(SignUpRequest signUpRequest, HttpServletRequest httpRequest) {
        // 1. Validate request data
        validateSignUpRequest(signUpRequest);

        // 2. Check if email is already in use
        if (userRepository.findByEmail(signUpRequest.email()).isPresent()) {
            throw new EmailAlreadyExistsException();
        }

        // 3. Create User entity from request
        User user = new User();
        user.setName(signUpRequest.name());
        user.setSurname(signUpRequest.surname());
        user.setEmail(signUpRequest.email());
        user.setPassword(passwordEncoder.encode(signUpRequest.password()));
        user.setRole(UserRole.valueOf(signUpRequest.role().toUpperCase()));

        try {
            // 4. Save the user to the database
            User savedUser = userRepository.save(user);

            // 5. Generate the accessToken
            String accessToken = jwtService.generateAccessToken(user);

            // 6. Create a new refreshToken
            RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(savedUser, httpRequest);

            // 7. Get the refreshToken token string
            String refreshToken = refreshTokenEntity.getToken();

            // 8. Get the expiration times for the tokens
            Long accessTokenExpiration = jwtService.getExpirationTimeInSeconds(accessToken);
            Long refreshTokenExpirationMs = jwtService.getExpirationTimeInSeconds(refreshToken);

            // 9. Return the response
            return new AuthenticationResponse(
                    accessToken,
                    refreshToken,
                    accessTokenExpiration,
                    refreshTokenExpirationMs,
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

    public AuthenticationResponse signIn(SignInRequest signInRequest, HttpServletRequest httpRequest) {
        try {
            // Use AuthenticationManager to validate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            signInRequest.email(),
                            signInRequest.password()));

            // If authentication was successful, get the user
            User user = userRepository.findByEmail(signInRequest.email())
                    .orElseThrow(() -> new UserNotFoundException(signInRequest.email()));

            // Generate the accessToken
            String accessToken = jwtService.generateAccessToken(user);

            // Create and store refresh token in database
            RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user, httpRequest);

            // Get the refreshToken token string
            String refreshToken = refreshTokenEntity.getToken();

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

        try {
            // Verify the refreshToken is valid
            RefreshToken storedTokenEntity = refreshTokenService.verifyRefreshToken(refreshToken);

            // Get the user from the refreshTokenEntity
            User user = storedTokenEntity.getUser();

            // Create a new refresh token (token rotation for security)
            RefreshToken newRefreshTokenEntity = refreshTokenService.rotateRefreshToken(storedTokenEntity, request);

            // Generate a new access token
            String newAccessToken = jwtService.generateAccessToken(user);

            // Get the new refreshToken token string
            String newRefreshToken = newRefreshTokenEntity.getToken();

            // Get expiration times
            Long accessTokenExpiration = jwtService.getExpirationTimeInSeconds(newAccessToken);

            Long refreshTokenExpiration = jwtService.getExpirationTimeInSeconds(newRefreshToken);

            // Return the response
            return ResponseEntity.ok(new AuthenticationResponse(
                    newAccessToken,
                    newRefreshToken,
                    accessTokenExpiration,
                    refreshTokenExpiration,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getSurname(),
                    user.getRole().name()));
        } catch (InvalidRefreshTokenException e) {
            return new ResponseEntity<AuthenticationResponse>(HttpStatus.UNAUTHORIZED);
        }
    }

    public void signOut(HttpServletRequest request) {
        try {
            // Get the authorization header from the request
            String authorizationHeader = request.getHeader("Authorization");
            // Check if the authrizationHeader is valid
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new InvalidRequestException("Invalid authorization header");
            }

            // Extract the refreshToken from the authorization header
            String refreshToken = authorizationHeader.substring(7);
            // Find the token

            // Check if it's a refresh token
            String tokenType = jwtService.extractTypeFromToken(refreshToken);
            if (!"refresh".equals(tokenType)) {
                throw new InvalidRequestException("Please provide refresh token for signout");
            }

            // Get user from token
            String username = jwtService.extractUsernameFromToken(refreshToken);
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UserNotFoundException(username));

            // Validate refresh token
            if (!jwtService.validateRefreshToken(refreshToken, user)) {
                throw new InvalidRequestException("Invalid refresh token");
            }

            refreshTokenService.findByToken(refreshToken)
                    .ifPresent(storedToken -> {
                        // Revoke this specific token
                        storedToken.setRevoked(true);
                        refreshTokenRepository.save(storedToken);

                        // Increment the user's tokenVersion
                        // to invalidate all access tokens too
                        user.incrementTokenVersion();
                        userRepository.save(user);
                    });
        } catch (Exception e) {
            throw new InvalidRequestException(e.getMessage());
        }

    }

    public void changePassword(String email, ChangePasswordRequest request) {
        // Validate request
        validateChangePasswordRequest(request);

        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Check if current password matches
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidRequestException("Current password is incorrect");
        }

        // Check if new password is different from current
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidRequestException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.newPassword()));

        // Increment token version to invalidate all existing tokens
        user.incrementTokenVersion();

        // Save user
        userRepository.save(user);

        // Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(user);
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

    private void validateChangePasswordRequest(ChangePasswordRequest request) {
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (!StringUtils.hasText(request.currentPassword())) {
            errors.add("Current password is required");
        }
        if (!StringUtils.hasText(request.newPassword())) {
            errors.add("New password is required");
        }
        if (!StringUtils.hasText(request.confirmNewPassword())) {
            errors.add("Password confirmation is required");
        }

        // Check if passwords match
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            errors.add("New password and confirmation do not match");
        }

        // Validate password strength
        if (StringUtils.hasText(request.newPassword()) && !isStrongPassword(request.newPassword())) {
            errors.add(
                    "New password must be strong: at least 8 characters, one uppercase, one lowercase, one number, and one special character");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
