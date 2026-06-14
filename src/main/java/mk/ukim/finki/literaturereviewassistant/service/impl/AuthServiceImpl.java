package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.model.AppUser;
import mk.ukim.finki.literaturereviewassistant.model.AuthSession;
import mk.ukim.finki.literaturereviewassistant.repository.AppUserRepository;
import mk.ukim.finki.literaturereviewassistant.repository.AuthSessionRepository;
import mk.ukim.finki.literaturereviewassistant.service.AuthService;
import mk.ukim.finki.literaturereviewassistant.web.dto.AuthResponse;
import mk.ukim.finki.literaturereviewassistant.web.dto.LoginRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.RegisterRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AuthSessionRepository sessionRepository;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        AppUser user = new AppUser();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());

        userRepository.save(user);
        return createSession(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(request.password())) {
            throw new RuntimeException("Invalid credentials");
        }

        return createSession(user);
    }

    @Override
    public UserResponse me(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token.isEmpty()) {
            throw new RuntimeException("Unauthorized");
        }
        AppUser user = sessionRepository.findByToken(token)
                .map(AuthSession::getUser)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        return toUserResponse(user);
    }

    @Override
    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (!token.isEmpty()) {
            sessionRepository.deleteById(token);
        }
    }

    private AuthResponse createSession(AppUser user) {
        String token = UUID.randomUUID().toString();

        AuthSession session = new AuthSession();
        session.setToken(token);
        session.setUser(user);
        session.setCreatedAt(LocalDateTime.now());

        sessionRepository.save(session);
        
        return new AuthResponse(session.getToken(), toUserResponse(user));
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId().toString(), user.getName(), user.getEmail());
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
