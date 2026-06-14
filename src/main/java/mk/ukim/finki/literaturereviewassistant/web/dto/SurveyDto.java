package mk.ukim.finki.literaturereviewassistant.web.dto;

import lombok.Data;

@Data
public class SurveyDto {
    private String id;
    private String name;
    private String description;
    private String createdDate;
    private String status;
    private int totalArticles;
}
