package com.marcovavassori.banking.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marcovavassori.banking.dtos.AuthenticationResponse;
import com.marcovavassori.banking.dtos.ChangePasswordRequest;
import com.marcovavassori.banking.dtos.SignInRequest;
import com.marcovavassori.banking.dtos.SignUpRequest;
import com.marcovavassori.banking.services.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthenticationResponse> signUp(@RequestBody SignUpRequest signUpRequest,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authenticationService.signUp(signUpRequest, httpRequest));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthenticationResponse> signIn(@RequestBody SignInRequest signInRequest,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authenticationService.signIn(signInRequest, httpRequest));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request,
            HttpServletResponse response) {
        return authenticationService.refreshToken(request, response);
    }

    @PostMapping("/signout")
    public ResponseEntity<Void> signOut(
            HttpServletRequest request) {
        authenticationService.signOut(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        // Get the email from the authenticated user using the Authentication object's
        // getName() method that returns the principal of the authenticated user. In my
        // case it returns the email of the authenticated user because i set it up this
        // way in JwtService with setSubject() in generateAccessToken() method.
        String email = authentication.getName();

        authenticationService.changePassword(email, request);
        return ResponseEntity.ok().build();
    }
}
