package mk.ukim.finki.literaturereviewassistant.web.dto;

public record PdfDownload(
        byte[] content,
        String mimeType,
        String fileName
) {
}
