package mk.ukim.finki.literaturereviewassistant.web.dto;

public record SurveyDto(
        String id,
        String name,
        String description,
        String createdDate,
        String status,
        int totalArticles,
        int reviewedArticlesCount
) {
}
