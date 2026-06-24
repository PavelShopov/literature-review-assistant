package mk.ukim.finki.literaturereviewassistant.service.DataService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfExtractorServiceTest {
    @Test
    void detectsDoiFromPdfText() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                content.showText("Paper DOI: 10.1000/Example.DOI");
                content.endText();
            }
            document.save(output);
            pdf = output.toByteArray();
        }

        ArticleMetadata metadata = new PdfExtractorService().extractFromBytes(pdf, "paper.pdf");
        assertEquals("10.1000/example.doi", metadata.getDoi());
    }

    @Test
    void extractsTitleAndAuthorsFromFirstPageText() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(50, 740);
                content.showText("A Practical Study of Example Extraction");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 710);
                content.showText("John Doe, Jane Smith");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 11);
                content.newLineAtOffset(50, 680);
                content.showText("Abstract: This paper evaluates title and author extraction.");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 11);
                content.newLineAtOffset(50, 650);
                content.showText("Introduction");
                content.endText();
            }
            document.save(output);
            pdf = output.toByteArray();
        }

        ArticleMetadata metadata = new PdfExtractorService().extractFromBytes(pdf, "paper.pdf");
        assertEquals("A Practical Study of Example Extraction", metadata.getTitle());
        assertEquals(2, metadata.getAuthorNames().size());
        assertTrue(metadata.getAuthorNames().contains("John Doe"));
        assertTrue(metadata.getAuthorNames().contains("Jane Smith"));
    }
}
