package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.AuthLoginRequest;
import com.example.bankcards.dto.AuthTokenRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.UserService;
import com.example.bankcards.security.JwtUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@ContextConfiguration(classes = {AuthController.class, TestSecurityConfig.class})
@AutoConfigureWebMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(User.Role.USER)
                .build();
    }

    @Test
    void login_ShouldReturnJwtToken() throws Exception {
        UserDetails userDetails = new JwtUser(testUser);
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(jwtUtil.generateToken(userDetails)).thenReturn("test-jwt-token");

        String requestBody = objectMapper.writeValueAsString(new AuthLoginRequest(
                "testuser",
                "password"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"));
    }

    @Test
    void validateToken_ShouldReturnValidWhenTokenIsValid() throws Exception {
        UserDetails userDetails = new JwtUser(testUser);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid-token", userDetails)).thenReturn(true);

        String requestBody = objectMapper.writeValueAsString(new AuthTokenRequest("valid-token"));

        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }

    @Test
    void validateToken_ShouldReturnInvalidWhenTokenIsInvalid() throws Exception {
        UserDetails userDetails = new JwtUser(testUser);
        when(jwtUtil.extractUsername("invalid-token")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("invalid-token", userDetails)).thenReturn(false);

        String requestBody = objectMapper.writeValueAsString(new AuthTokenRequest("invalid-token"));

        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid token"));
    }
}
