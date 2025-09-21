package com.example.bankcards.service;

import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DuplicateUsernameException;
import com.example.bankcards.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(User user) {
        log.info("Creating user: {}", user.getUsername());

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateUsernameException(user.getUsername());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        log.info("User created successfully with id: {}", savedUser.getId());
        return savedUser;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
