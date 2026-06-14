package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.model.AppUser;
import mk.ukim.finki.literaturereviewassistant.web.dto.AuthResponseDto;

public interface AuthService {
    AuthResponseDto register(String name, String email, String password);
    AuthResponseDto login(String email, String password);
    void logout(String token);
    AppUser getUserByToken(String token);
}
