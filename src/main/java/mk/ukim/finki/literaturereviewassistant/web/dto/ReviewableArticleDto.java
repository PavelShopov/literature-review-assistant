package mk.ukim.finki.literaturereviewassistant.web.dto;

import java.util.List;

/**
 * Represents an article in the context of a reviewer's review queue.
 * Includes whether the reviewer has already submitted a review and what jsonResponse was saved.
 */
public record ReviewableArticleDto(
        String externalId,
        String title,
        String journal,
        Integer publicationYear,
        String doi,
        String url,
        String status,
        String articleAbstract,
        String inclusionSummary,
        List<String> authors,
        boolean reviewed,
        String jsonResponse,
        List<AIAnnotationDto> aiAnnotations
) {
}
