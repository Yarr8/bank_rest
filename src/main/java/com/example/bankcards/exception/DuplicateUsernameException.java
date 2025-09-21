package com.example.bankcards.exception;

public class DuplicateUsernameException extends BusinessException {

    public DuplicateUsernameException(String username) {
        super("Username already exists: " + username);
    }
}
