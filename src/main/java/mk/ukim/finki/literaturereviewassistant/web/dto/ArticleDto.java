package mk.ukim.finki.literaturereviewassistant.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ArticleDto(
        String id,
        String title,
        List<String> authors,
        String journal,
        Integer year,
        String doi,
        String status,
        @JsonProperty("abstract")
        String abstractText,
        String inclusionSummary,
        AddedByDto addedBy,
        String url,
        String openUrl,
        String openType,
        boolean hasPdf,
        String pdfUrl
) {
}
