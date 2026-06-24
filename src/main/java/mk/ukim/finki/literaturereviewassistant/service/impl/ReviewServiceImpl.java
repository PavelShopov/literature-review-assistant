package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Review;
import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewerRepository;
import mk.ukim.finki.literaturereviewassistant.repository.SurveyRepository;
import mk.ukim.finki.literaturereviewassistant.service.AuthService;
import mk.ukim.finki.literaturereviewassistant.service.ReviewService;
import mk.ukim.finki.literaturereviewassistant.web.dto.ReviewableArticleDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.UserResponse;
import mk.ukim.finki.literaturereviewassistant.web.dto.ReviewRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewerRepository reviewerRepository;
    private final SurveyRepository surveyRepository;
    private final ArticleRepository articleRepository;
    private final AuthService authService;

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
    }

    @Override
    @Transactional
    public Review addReview(String authorizationHeader, String surveyId, String articleId, ReviewRequest request) {
        // Authenticate user
        UserResponse user = authService.me(authorizationHeader);
        if (user == null || user.email() == null) {
            return null;
        }
        Survey survey = surveyRepository.findByExternalId(surveyId).orElse(null);
        if (survey == null) {
            return null;
        }
        Reviewer reviewer = reviewerRepository.findByEmail(user.email()).stream()
                .filter(r -> r.getSurveys().contains(survey) && "Reviewer".equals(r.getRole()))
                .findFirst()
                .orElse(null);
        if (reviewer == null) {
            return null;
        }
        Article article = articleRepository.findByExternalId(articleId).orElse(null);
        if (article == null) {
            return null;
        }
        boolean belongs = survey.getArticleLinks().stream()
                .anyMatch(link -> link.getArticle() != null && link.getArticle().getArticleId().equals(article.getArticleId()));
        if (!belongs) {
            return null;
        }
        Review review = new Review();
        review.setJsonResponse(request.getJsonResponse());
        review.setArticle(article);
        review.setReviewer(reviewer);
        return reviewRepository.save(review);
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
                if (survey.getArticleLinks() == null || survey.getArticleLinks().isEmpty()) {
                    continue;
                }
                List<Review> reviews = reviewRepository.findByReviewer(reviewer);
                Set<Long> reviewedArticleIds = reviews.stream()
                        .map(r -> r.getArticle().getArticleId())
                        .collect(Collectors.toSet());
                boolean hasPending = survey.getArticleLinks().stream()
                        .anyMatch(as -> !reviewedArticleIds.contains(as.getArticle().getArticleId()));
                if (hasPending) {
                    surveysToReview.add(toSurveyDto(survey));
                }
            }
        }
        return surveysToReview;
    }

    @Override
    public List<ReviewableArticleDto> getArticlesForReview(String authorizationHeader, String surveyId) {
        return List.of();
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
}