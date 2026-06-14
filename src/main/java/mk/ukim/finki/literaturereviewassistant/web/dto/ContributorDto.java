package mk.ukim.finki.literaturereviewassistant.web.dto;

import lombok.Data;

@Data
public class ContributorDto {
    private String id;
    private String name;
    private String email;
    private String role;
    private String addedDate;
}
