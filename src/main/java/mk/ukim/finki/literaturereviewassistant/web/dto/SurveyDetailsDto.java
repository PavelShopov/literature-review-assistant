package mk.ukim.finki.literaturereviewassistant.web.dto;

public record SurveyDetailsDto(
        String id,
        String name,
        String description,
        String researchQuestion,
        String createdDate,
        String status,
        int totalArticles,
        int screened,
        int pending,
        UserResponse owner
) {
}
