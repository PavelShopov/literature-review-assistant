package mk.ukim.finki.literaturereviewassistant.web.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReviewSubmissionDto {
    private Map<String, UserAnnotationDto> reviewForm;
}

