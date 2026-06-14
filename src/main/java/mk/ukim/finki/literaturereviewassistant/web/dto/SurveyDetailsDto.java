package mk.ukim.finki.literaturereviewassistant.web.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SurveyDetailsDto extends SurveyDto {
    private String researchQuestion;
    private int screened;
    private int pending;
    private OwnerDto owner;

    @Data
    public static class OwnerDto {
        private String name;
        private String email;
    }
}
