package mk.ukim.finki.literaturereviewassistant.web.dto;

public record ArticleImportRequest(
        String type,
        String data,
        AddedByDto addedBy,
        String fileName,
        String mimeType
) {
}
