package mk.ukim.finki.literaturereviewassistant.web.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String email;
    private String password;
}
