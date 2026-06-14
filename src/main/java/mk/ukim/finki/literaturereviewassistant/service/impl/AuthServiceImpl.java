package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.model.AppUser;
import mk.ukim.finki.literaturereviewassistant.model.AuthSession;
import mk.ukim.finki.literaturereviewassistant.repository.AppUserRepository;
import mk.ukim.finki.literaturereviewassistant.repository.AuthSessionRepository;
import mk.ukim.finki.literaturereviewassistant.service.AuthService;
import mk.ukim.finki.literaturereviewassistant.web.dto.AuthResponseDto;
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
    public AuthResponseDto register(String name, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        // Note: Password should be hashed in a real application.
        // For simplicity with this mock/bearer implementation without Spring Security:
        user.setPassword(password);
        
        userRepository.save(user);
        return createSession(user);
    }

    @Override
    public AuthResponseDto login(String email, String password) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid credentials");
        }
        
        return createSession(user);
    }

    @Override
    public void logout(String token) {
        sessionRepository.deleteById(token);
    }

    @Override
    public AppUser getUserByToken(String token) {
        return sessionRepository.findByToken(token)
                .map(AuthSession::getUser)
                .orElse(null);
    }

    private AuthResponseDto createSession(AppUser user) {
        String token = UUID.randomUUID().toString();
        
        AuthSession session = new AuthSession();
        session.setToken(token);
        session.setUser(user);
        session.setCreatedAt(LocalDateTime.now());
        
        sessionRepository.save(session);
        
        AuthResponseDto.UserDto userDto = new AuthResponseDto.UserDto(
                user.getId().toString(),
                user.getName(),
                user.getEmail()
        );
        
        return new AuthResponseDto(token, userDto);
    }
}
