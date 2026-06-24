package mk.ukim.finki.literaturereviewassistant.web.dto;

import java.util.List;

public record ArticleUpdateRequest(
        String title,
        List<String> authors,
        String journal,
        Integer year,
        String doi,
        String status,
        String abstractText,
        String inclusionSummary
) {
}
