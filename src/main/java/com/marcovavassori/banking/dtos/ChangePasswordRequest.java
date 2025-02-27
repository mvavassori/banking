package com.marcovavassori.banking.dtos;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword,
        String confirmNewPassword) {
}