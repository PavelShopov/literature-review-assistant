package mk.ukim.finki.literaturereviewassistant.web.dto;

public record ReviewerDto(
        String id,
        String name,
        String email,
        String role,
        String addedDate
) {
}
