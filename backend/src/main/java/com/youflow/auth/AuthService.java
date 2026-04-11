package com.youflow.auth;

import com.youflow.auth.dto.AuthResponse;
import com.youflow.auth.dto.LoginRequest;
import com.youflow.auth.dto.RegisterRequest;
import com.youflow.user.User;
import com.youflow.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User user = new User(req.email(), passwordEncoder.encode(req.password()));
        userRepository.save(user);
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return new AuthResponse(jwtUtil.generateToken(user.getEmail()));
    }
}
