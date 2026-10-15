package com.choresync.service;

import com.choresync.dto.AuthDtos.AuthResponse;
import com.choresync.entity.User;
import com.choresync.exception.BusinessException;
import com.choresync.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public AuthResponse getProfile() {
        User user = authService.getCurrentUserOrThrow();
        return new AuthResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getDisplayName());
    }

    @Transactional
    public AuthResponse updateProfile(Map<String, String> body) {
        User user = authService.getCurrentUserOrThrow();
        if (body.containsKey("firstName")) {
            user.setFirstName(body.get("firstName"));
        }
        if (body.containsKey("lastName")) {
            user.setLastName(body.get("lastName"));
        }
        if (body.containsKey("displayName") && !body.get("displayName").isBlank()) {
            user.setDisplayName(body.get("displayName"));
        } else if (user.getFirstName() != null) {
            user.setDisplayName((user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")).trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        return new AuthResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getDisplayName());
    }
}
