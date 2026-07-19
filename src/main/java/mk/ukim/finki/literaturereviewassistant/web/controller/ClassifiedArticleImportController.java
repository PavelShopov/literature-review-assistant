package mk.ukim.finki.literaturereviewassistant.web.controller;

import mk.ukim.finki.literaturereviewassistant.service.ClassifiedArticleImportService;
import mk.ukim.finki.literaturereviewassistant.web.dto.ClassifiedArticleImportSummary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/surveys/{surveyId}/classified-articles")
public class ClassifiedArticleImportController {

    private final ClassifiedArticleImportService classifiedArticleImportService;

    public ClassifiedArticleImportController(ClassifiedArticleImportService classifiedArticleImportService) {
        this.classifiedArticleImportService = classifiedArticleImportService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClassifiedArticleImportSummary importClassifiedArticles(
            @PathVariable String surveyId,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return classifiedArticleImportService.importClassifiedArticles(surveyId, file);
    }
}
