package mk.ukim.finki.literaturereviewassistant.web.dto;

import lombok.Data;

@Data
public class RegisterRequestDto {
    private String name;
    private String email;
    private String password;
}
