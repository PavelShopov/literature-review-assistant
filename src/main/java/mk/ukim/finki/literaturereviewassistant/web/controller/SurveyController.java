package mk.ukim.finki.literaturereviewassistant.web.controller;

import mk.ukim.finki.literaturereviewassistant.model.*;
import mk.ukim.finki.literaturereviewassistant.repository.AppUserRepository;
import mk.ukim.finki.literaturereviewassistant.service.SurveyService;
import mk.ukim.finki.literaturereviewassistant.web.dto.*;
import mk.ukim.finki.literaturereviewassistant.service.SurveyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;
    private final AppUserRepository userRepository;

    public SurveyController(SurveyService surveyService, AppUserRepository userRepository) {
        this.surveyService = surveyService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<SurveyDto> getAllSurveys(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return surveyService.findAllSurveys(authorizationHeader);
    }
//    @GetMapping
//    public List<SurveyDto> getAllUsersSurveys(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, String userID) {
//        return surveyService;
//    }

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
        // Your service implementation already returns an ArticleDto!
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

    @GetMapping("/{surveyId}/articles/{articleId}")
    public ArticleDto getArticleDetails(
            @PathVariable String surveyId,
            @PathVariable String articleId) {
        return surveyService.findArticle(articleId);
    }

    @PostMapping("/{surveyId}/contributors")
    public ReviewerDto addReviewer(
            @PathVariable String surveyId,
            @RequestBody ReviewerDto reviewer,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        // Let the service handle the entity conversion and return the DTO directly
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

    @GetMapping("/reviewers/search")
    public List<ReviewerDto> searchReviewers(@RequestParam String email) {
        return surveyService.searchReviewersByEmail(email);
    }

    private SurveyDetailsDto toSurveyDetailsDto(Survey survey) {
        // 1. Иницијализација на бројачи за статистика на артиклите
        int totalArticles = 0;
        int screened = 0;
        int pending = 0;

        if (survey.getArticleLinks() != null) {
            totalArticles = survey.getArticleLinks().size();
            for (ArticleSurvey link : survey.getArticleLinks()) {
                // Ако статусот е PENDING, го броиме како pending, во спротивно е поминат (screened)
                if (link.getStatus() != null && "PENDING".equalsIgnoreCase(link.getStatus().toString())) {
                    pending++;
                } else {
                    screened++;
                }
            }
        }

        // 2. Издвојување на сопственикот (Owner) од рецензентите
        UserResponse ownerDto = null;
        if (survey.getReviewers() != null) {
            for (Reviewer r : survey.getReviewers()) {
                if ("Owner".equalsIgnoreCase(r.getRole())) {
                    ownerDto = new UserResponse(
                            r.getReviewerId(),
                            r.getName(),
                            r.getEmail()
                    );
                    break;
                }
            }
        }

        // Fallback ако случајно нема дефинирано Owner во базата за оваа анкета
        if (ownerDto == null) {
            ownerDto = new UserResponse(-1L, "Survey Owner", "owner@finki.ukim.mk");
        }

        // 3. Сега ги праќаме точно 10-те аргументи во редоследот кој го бара твојот рекорд
        return new SurveyDetailsDto(
                survey.getExternalId(),                                                      // 1. id (String)
                survey.getTitle(),                                                           // 2. name (String)
                survey.getDescription(),                                                     // 3. description (String)
                survey.getResearchQuestion(),                                                 // 4. researchQuestion (String)
                survey.getCreatedDate() != null ? survey.getCreatedDate().toString() : null, // 5. createdDate (String)
                survey.getStatus(),                                                          // 6. status (String)
                totalArticles,                                                               // 7. totalArticles (int)
                screened,                                                                    // 8. screened (int)
                pending,                                                                     // 9. pending (int)
                ownerDto                                                                     // 10. owner (UserResponse)
        );
    }
}
