package mk.ukim.finki.literaturereviewassistant.web.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserAnnotationDto {
    private String dimension;
    private List<String> values;
    private String confidence;
    private String proof;
}
