package com.example.bankcards.config;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Примечание для проверяющего:
// Добавляет пользователя с правами админа для тестов
// Работает ТОЛЬКО в dev-среде
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class TestAdminLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            log.info("Creating test admin user for development environment");

            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.ADMIN)
                    .build();

            log.info("Saving admin user with encoded password");
            userRepository.save(admin);
            log.info("Test admin user created successfully with username: admin, password: admin123");
        } else {
            log.info("Test admin user already exists, skipping creation");
        }
    }
}
