package com.rawr.auth;

import com.rawr.user.User;
import com.rawr.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "username", user.getUsername(),
                "profileImage", user.getProfileImage() != null ? user.getProfileImage() : "",
                "role", user.getRole()
        ));
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, String>> invite(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        userRepository.findByEmail(email).ifPresent(User::promoteToContributor);
        return ResponseEntity.ok(Map.of("message", "Contributor invited: " + email));
    }
}
