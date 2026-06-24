package mk.ukim.finki.literaturereviewassistant.web.dto;

public record SurveyRequest(
        String id,
        String name,
        String description,
        String createdDate,
        String status,
        Integer totalArticles,
        String researchQuestion
) {
}
