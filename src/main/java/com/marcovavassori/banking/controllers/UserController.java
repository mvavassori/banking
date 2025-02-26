package com.marcovavassori.banking.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @RequestMapping("/profile")
    public ResponseEntity<String> user() {
        return ResponseEntity.ok("User endpoint");
    }

    @RequestMapping("/admin")
    public ResponseEntity<String> admin() {
        return ResponseEntity.ok("Admin endpoint");
    }
}
