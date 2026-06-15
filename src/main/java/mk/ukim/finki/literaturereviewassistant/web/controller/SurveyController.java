package mk.ukim.finki.literaturereviewassistant.web.controller;

import mk.ukim.finki.literaturereviewassistant.service.SurveyService;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.AddedByDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleImportRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleUpdateRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.ReviewerDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDetailsDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyAskRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping
    public List<SurveyDto> getAllSurveys() {
        return surveyService.findAllSurveys();
    }

    @GetMapping("/{surveyId}")
    public SurveyDetailsDto getSurveyById(@PathVariable String surveyId) {
        return surveyService.findSurvey(surveyId);
    }

    @PutMapping("/{surveyId}")
    public SurveyDto saveSurvey(
            @PathVariable String surveyId,
            @RequestBody SurveyRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return surveyService.saveSurvey(surveyId, request, authorization);
    }

    @DeleteMapping("/{surveyId}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable String surveyId) {
        surveyService.deleteSurvey(surveyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{surveyId}/articles")
    public List<ArticleDto> getSurveyArticles(@PathVariable String surveyId) {
        return surveyService.findArticles(surveyId);
    }

    @PostMapping("/{surveyId}/articles/import")
    public ArticleDto importArticle(
            @PathVariable String surveyId,
            @RequestBody ArticleImportRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return surveyService.importArticle(surveyId, request, authorization);
    }

    @PostMapping(value = "/{surveyId}/articles/import/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArticleDto importPdfArticle(
            @PathVariable String surveyId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "addedById", required = false) String addedById,
            @RequestPart(value = "addedByName", required = false) String addedByName,
            @RequestPart(value = "addedByRole", required = false) String addedByRole
    ) {
        AddedByDto addedBy = new AddedByDto(
                addedById == null || addedById.isBlank() ? "owner" : addedById,
                addedByName == null || addedByName.isBlank() ? "Survey Owner" : addedByName,
                addedByRole == null || addedByRole.isBlank() ? "Owner" : addedByRole
        );
        return surveyService.importPdfArticle(surveyId, file, addedBy);
    }

    @PostMapping("/{surveyId}/ask")
    public String askSurvey(@PathVariable String surveyId, @RequestBody SurveyAskRequest request) {
        return surveyService.askSurvey(surveyId, request);
    }

    @PutMapping("/{surveyId}/articles/{articleId}")
    public ArticleDto updateArticle(
            @PathVariable String surveyId,
            @PathVariable String articleId,
            @RequestBody ArticleUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return surveyService.updateArticle(surveyId, articleId, request, authorization);
    }

    @DeleteMapping("/{surveyId}/articles/{articleId}")
    public ResponseEntity<Void> deleteArticle(
            @PathVariable String surveyId,
            @PathVariable String articleId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        surveyService.deleteArticle(surveyId, articleId, authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/articles/{articleId}")
    public ArticleDto getArticleById(@PathVariable String articleId) {
        return surveyService.findArticle(articleId);
    }

    @GetMapping("/{surveyId}/contributors")
    public List<ReviewerDto> getReviewers(@PathVariable String surveyId) {
        return surveyService.findReviewers(surveyId);
    }

    @PostMapping("/{surveyId}/contributors")
    public ReviewerDto addReviewer(
            @PathVariable String surveyId,
            @RequestBody ReviewerDto reviewer,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return surveyService.addReviewer(surveyId, reviewer, authorization);
    }

    @DeleteMapping("/{surveyId}/contributors/{reviewerId}")
    public ResponseEntity<Void> removeReviewer(
            @PathVariable String surveyId,
            @PathVariable String reviewerId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        surveyService.removeReviewer(surveyId, reviewerId, authorization);
        return ResponseEntity.noContent().build();
    }
}
