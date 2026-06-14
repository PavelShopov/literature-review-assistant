package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.model.AppUser;
import mk.ukim.finki.literaturereviewassistant.web.dto.AuthResponseDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.AuthResponse;
import mk.ukim.finki.literaturereviewassistant.web.dto.LoginRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.RegisterRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse me(String authorizationHeader);

    void logout(String authorizationHeader);
}
