package com.example.bankcards.controller;

import com.example.bankcards.dto.AuthLoginRequest;
import com.example.bankcards.dto.AuthTokenRequest;
import com.example.bankcards.dto.GenericErrorResponse;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.security.UserDetailsServiceImpl;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthLoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            log.info("Login successful for user: {}", request.getUsername());

            return ResponseEntity.ok(new LoginResponse(token, "Login successful"));

        } catch (Exception e) {
            log.error("Login failed for user: {} - {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Invalid username or password"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody AuthTokenRequest request) {
        try {
            String username = jwtUtil.extractUsername(request.getToken());
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            boolean isValid = jwtUtil.validateToken(request.getToken(), userDetails);

            if (isValid) {
                return ResponseEntity.ok(new TokenValidationResponse(true, "Token is valid"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new GenericErrorResponse("Invalid token"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Token validation failed"));
        }
    }

    // Response classes
    @Data
    public static class LoginResponse {
        private final String token;
        private final String message;
    }


    @Data
    public static class TokenValidationResponse {
        private final boolean valid;
        private final String message;
    }
}
