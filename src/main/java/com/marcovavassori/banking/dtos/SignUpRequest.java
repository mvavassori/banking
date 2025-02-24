package com.marcovavassori.banking.dtos;

public record SignUpRequest(
        String name,
        String surname,
        String email,
        String password,
        String role) {

}
