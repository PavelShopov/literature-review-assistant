package mk.ukim.finki.literaturereviewassistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.ArticleStatus;
import mk.ukim.finki.literaturereviewassistant.model.Review;
import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewerRepository;
import mk.ukim.finki.literaturereviewassistant.repository.SurveyRepository;
import mk.ukim.finki.literaturereviewassistant.service.AuthService;
import mk.ukim.finki.literaturereviewassistant.service.ReviewService;
import mk.ukim.finki.literaturereviewassistant.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewerRepository reviewerRepository;
    private final SurveyRepository surveyRepository;
    private final ArticleRepository articleRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ReviewerRepository reviewerRepository,
                             SurveyRepository surveyRepository,
                             ArticleRepository articleRepository,
                             AuthService authService) {
        this.reviewRepository = reviewRepository;
        this.reviewerRepository = reviewerRepository;
        this.surveyRepository = surveyRepository;
        this.articleRepository = articleRepository;
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public Review addReview(String authorizationHeader, String surveyId, String articleId, ReviewRequest request) {
        UserResponse user = authService.me(authorizationHeader);
        if (user == null || user.email() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Survey survey = surveyRepository.findByExternalId(surveyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + surveyId));
        Reviewer reviewer = reviewerRepository.findByEmail(user.email()).stream()
                .filter(r -> r.getSurveys().contains(survey) && "Reviewer".equals(r.getRole()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this survey as a reviewer"));
        Article article = articleRepository.findByExternalId(articleId)
                .filter(found -> survey.getArticleLinks().stream()
                        .anyMatch(link -> link.getArticle() != null && link.getArticle().getArticleId().equals(found.getArticleId())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found: " + articleId));
        boolean belongs = survey.getArticleLinks().stream()
                .anyMatch(link -> link.getArticle() != null && link.getArticle().getArticleId().equals(article.getArticleId()));
        if (!belongs) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found: " + articleId);
        }
        
        Review review = reviewRepository.findByReviewerAndArticle(reviewer, article).orElse(new Review());
        if (request == null || request.getJsonResponse() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review payload is required");
        }
        String decision = extractDecision(request.getJsonResponse());
        applyDecisionToArticle(article, survey, decision);
        review.setJsonResponse(request.getJsonResponse());
        review.setArticle(article);
        review.setReviewer(reviewer);
        reviewRepository.save(review);
        articleRepository.save(article);
        return review;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyDto> getSurveysToReview(String authorizationHeader) {
        UserResponse user = authService.me(authorizationHeader);
        if (user == null || user.email() == null) {
            return List.of();
        }
        List<Reviewer> reviewers = reviewerRepository.findByEmail(user.email());
        if (reviewers == null || reviewers.isEmpty()) {
            return List.of();
        }

        List<SurveyDto> surveysToReview = new ArrayList<>();

        for (Reviewer reviewer : reviewers) {
            for (Survey survey : reviewer.getSurveys()) {
                // Skip surveys that this user created (is the Owner of).
                // A user is the owner if they are the ONLY reviewer for this survey with role "Owner"
                // AND their email matches. We detect ownership by checking if all "Owner"-role
                // reviewers on this survey have this user's email.
                boolean isOwner = reviewerRepository.findBySurveysContaining(survey).stream()
                        .anyMatch(r -> "Owner".equals(r.getRole())
                                && r.getEmail().equalsIgnoreCase(user.email()));
                if (isOwner) {
                    continue;
                }

                // Include this survey only if there are pending (unreviewed) articles
//                if (survey.getArticleLinks() == null || survey.getArticleLinks().isEmpty()) {
//                    continue;
//                }
                List<Review> reviews = reviewRepository.findByReviewer(reviewer);
                Set<Long> reviewedArticleIds = reviews.stream()
                        .map(r -> r.getArticle().getArticleId())
                        .collect(Collectors.toSet());
//                boolean hasPending = survey.getArticleLinks().stream()
//                        .anyMatch(as -> !reviewedArticleIds.contains(as.getArticle().getArticleId()));
//                if (hasPending) {
//                }
                surveysToReview.add(toSurveyDto(survey));
            }
        }
        return surveysToReview;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewableArticleDto> getArticlesForReview(String authorizationHeader, String surveyId) {
        UserResponse user = authService.me(authorizationHeader);
        if (user == null || user.email() == null) {
            return List.of();
        }
        Survey survey = surveyRepository.findByExternalId(surveyId).orElse(null);
        if (survey == null) {
            return List.of();
        }
        Reviewer reviewer = reviewerRepository.findByEmail(user.email()).stream()
                .filter(r -> r.getSurveys().contains(survey) && "Reviewer".equals(r.getRole()))
                .findFirst()
                .orElse(null);
        if (reviewer == null) {
            return List.of();
        }

        List<ReviewableArticleDto> dtos = new ArrayList<>();
        List<Review> userReviews = reviewRepository.findByReviewer(reviewer);

        if (survey.getArticleLinks() != null) {
            for (var link : survey.getArticleLinks()) {
                Article article = link.getArticle();
                if (article != null) {
                    Review existingReview = userReviews.stream()
                            .filter(r -> r.getArticle().getArticleId().equals(article.getArticleId()))
                            .findFirst()
                            .orElse(null);

                    List<mk.ukim.finki.literaturereviewassistant.web.dto.AIAnnotationDto> aiAnnotations = new ArrayList<>();
                    if (article.getAnnotationResults() != null) {
                        for (mk.ukim.finki.literaturereviewassistant.model.AnnotationResult res : article.getAnnotationResults()) {
                            String promptText = res.getPrompt() != null ? res.getPrompt().getPromptText() : "Unknown Prompt";
                            aiAnnotations.add(new mk.ukim.finki.literaturereviewassistant.web.dto.AIAnnotationDto(promptText, res.getJsonResponse()));
                        }
                    }

                    dtos.add(new ReviewableArticleDto(
                            article.getExternalId(),
                            article.getTitle(),
                            article.getJournal(),
                            article.getPublicationYear(),
                            article.getDoi(),
                            article.getUrl(),
                            article.getStatus(),
                            article.getArticleAbstract(),
                            article.getInclusionSummary(),
                            article.getAuthors().stream().map(a -> a.getAuthorName()).toList(),
                            existingReview != null,
                            existingReview != null ? existingReview.getJsonResponse() : null,
                            aiAnnotations,
                            reviewerNameForArticle(article),
                            existingReview != null ? reviewDisplayText(existingReview.getJsonResponse()) : null
                    ));
                }
            }
        }
        dtos.sort((left, right) -> {
            int leftRank = left.reviewed() ? 1 : 0;
            int rightRank = right.reviewed() ? 1 : 0;
            if (leftRank != rightRank) {
                return Integer.compare(leftRank, rightRank);
            }
            String leftTitle = left.title() == null ? "" : left.title();
            String rightTitle = right.title() == null ? "" : right.title();
            return leftTitle.compareToIgnoreCase(rightTitle);
        });
        return dtos;
    }

    private SurveyDto toSurveyDto(Survey survey) {
        return new SurveyDto(
                survey.getExternalId(),
                survey.getTitle(),
                survey.getDescription(),
                survey.getCreatedDate() != null ? survey.getCreatedDate().toString() : "",
                survey.getStatus(),
                survey.getArticleLinks() != null ? survey.getArticleLinks().size() : 0
        );
    }

    private String extractDecision(String jsonResponse) {
        try {
            JsonNode node = objectMapper.readTree(jsonResponse);
            JsonNode decisionNode = node.get("decision");
            return decisionNode == null ? "" : decisionNode.asText("");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review payload");
        }
    }

    private void applyDecisionToArticle(Article article, Survey survey, String decision) {
        if (decision == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review decision is required");
        }

        String normalized = decision.trim().toLowerCase();
        if ("approve".equals(normalized)) {
            article.setStatus("INCLUDED");
            updateSurveyLinkStatus(article, survey, ArticleStatus.INCLUDED);
            return;
        }
        if ("deny".equals(normalized)) {
            article.setStatus("EXCLUDED");
            updateSurveyLinkStatus(article, survey, ArticleStatus.EXCLUDED);
            return;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review decision must be approve or deny");
    }

    private void updateSurveyLinkStatus(Article article, Survey survey, ArticleStatus status) {
        if (article.getSurveyLinks() == null) {
            return;
        }
        for (var link : article.getSurveyLinks()) {
            if (link.getSurvey() != null && survey.getExternalId() != null
                    && survey.getExternalId().equals(link.getSurvey().getExternalId())) {
                link.setStatus(status);
            }
        }
    }

    private String reviewDisplayText(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(jsonResponse);
            JsonNode note = node.get("note");
            if (note != null && !note.asText("").isBlank()) {
                return note.asText("");
            }
            JsonNode decision = node.get("decision");
            if (decision != null && !decision.asText("").isBlank()) {
                return decision.asText("");
            }
        } catch (Exception ignored) {
        }
        return jsonResponse;
    }

    private String reviewerNameForArticle(Article article) {
        if (article == null || article.getArticleId() == null) {
            return null;
        }
        return reviewRepository.findTopByArticle_ArticleIdOrderByReviewIdDesc(article.getArticleId())
                .map(review -> review.getReviewer() != null ? review.getReviewer().getName() : null)
                .orElse(null);
    }

    @Override
    @Transactional
    public Review saveOrUpdateReview(Long articleId, Long reviewerId, ReviewSubmissionDto submissionDto) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleId));

        Reviewer reviewer = reviewerRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerId));

        // Serialize the Map structure cleanly into a native JSON String
        String jsonText;
        try {
            jsonText = objectMapper.writeValueAsString(submissionDto.getReviewForm());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing taxonomy form values to JSON string", e);
        }

        // Fetch existing or initialize a fresh model object
        Review review = reviewRepository.findByArticleAndReviewer(article, reviewer)
                .orElse(new Review());

        review.setArticle(article);
        review.setReviewer(reviewer);
        review.setJsonResponse(jsonText);

        return reviewRepository.save(review);
    }

    @Override
    public Optional<Review> getReviewByArticleAndReviewer(Long articleId, Long reviewerId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleId));
        Reviewer reviewer = reviewerRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerId));

        return reviewRepository.findByArticleAndReviewer(article, reviewer);
    }
}
