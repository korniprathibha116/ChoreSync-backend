package com.choresync.service;

import com.choresync.dto.AuthDtos.AuthResponse;
import com.choresync.dto.AuthDtos.LoginRequest;
import com.choresync.dto.AuthDtos.RegisterRequest;
import com.choresync.entity.User;
import com.choresync.exception.BusinessException;
import com.choresync.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new BusinessException("Registration details are required.");
        }
        String email = request.getEmail().trim();
        String firstName = request.getFirstName() == null ? "" : request.getFirstName().trim();
        String lastName = request.getLastName() == null ? "" : request.getLastName().trim();
        String password = request.getPassword();

        if (email.isBlank() || password.isBlank()) {
            throw new BusinessException("Email and password are required.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("An account with that email already exists.");
        }

        String displayName = firstName.isBlank() && lastName.isBlank() ? email.split("@")[0] : (firstName + " " + lastName).trim();
        User user = new User(firstName, lastName, email, passwordEncoder.encode(password), displayName);
        user = userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password)
        );
        org.springframework.security.core.context.SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (httpRequest != null) {
            httpRequest.getSession(true).setAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
            );
        }

        return new AuthResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getDisplayName());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return register(request, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new BusinessException("Email and password are required.");
        }
        String email = request.getEmail().trim();
        String password = request.getPassword();

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password)
        );
        org.springframework.security.core.context.SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (httpRequest != null) {
            httpRequest.getSession(true).setAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
            );
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("User not found."));

        return new AuthResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getDisplayName());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, null);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String email = authentication.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    public User getCurrentUserOrThrow() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("Authentication required.");
        }
        return currentUser;
    }
}
