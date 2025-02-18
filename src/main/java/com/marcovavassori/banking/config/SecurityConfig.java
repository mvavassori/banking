package com.marcovavassori.banking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// `@Configuration` Marks this class as a configuration class for Spring. I.e. a source of bean definitions
@Configuration
public class SecurityConfig {

    @Bean // Indicates that a method produces a bean to be managed by the Spring container
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCryptPasswordEncoder is a strong password hashing algorithm
    }
}