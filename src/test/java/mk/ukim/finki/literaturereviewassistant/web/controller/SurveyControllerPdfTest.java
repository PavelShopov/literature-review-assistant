package mk.ukim.finki.literaturereviewassistant.web.controller;

import mk.ukim.finki.literaturereviewassistant.repository.AppUserRepository;
import mk.ukim.finki.literaturereviewassistant.repository.SurveyRepository;
import mk.ukim.finki.literaturereviewassistant.service.SurveyService;
import mk.ukim.finki.literaturereviewassistant.web.dto.PdfDownload;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SurveyControllerPdfTest {
    @Test
    void returnsPdfBytesWithSafeInlineHeaders() {
        SurveyService service = mock(SurveyService.class);
        when(service.findArticlePdf("article-1", "Bearer token"))
                .thenReturn(new PdfDownload("%PDF-test".getBytes(), MediaType.APPLICATION_PDF_VALUE, "paper.pdf"));

        SurveyController controller = new SurveyController(service, mock(AppUserRepository.class), mock(SurveyRepository.class));
        ResponseEntity<byte[]> response = controller.getArticlePdf("article-1", "Bearer token");

        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").startsWith("inline"));
        assertArrayEquals("%PDF-test".getBytes(), response.getBody());
    }
}
