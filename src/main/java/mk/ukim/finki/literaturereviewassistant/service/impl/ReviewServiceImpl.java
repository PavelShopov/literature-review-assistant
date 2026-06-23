package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.model.Review;
import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import mk.ukim.finki.literaturereviewassistant.model.ArticleSurvey;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewerRepository;
import mk.ukim.finki.literaturereviewassistant.service.AuthService;
import mk.ukim.finki.literaturereviewassistant.service.ReviewService;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.UserResponse;
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
    private final AuthService authService;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ReviewerRepository reviewerRepository, AuthService authService) {
        this.reviewRepository = reviewRepository;
        this.reviewerRepository = reviewerRepository;
        this.authService = authService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyDto> getSurveysToReview(String authorizationHeader) {
        UserResponse user = authService.me(authorizationHeader);
        if (user == null || user.email() == null) {
            return List.of();
        }

        Reviewer reviewer = reviewerRepository.findByEmail(user.email()).orElse(null);
        if (reviewer == null) {
            return List.of();
        }

        List<Review> reviews = reviewRepository.findByReviewer(reviewer);
        Set<Long> reviewedArticleIds = reviews.stream()
                .map(r -> r.getArticle().getArticleId())
                .collect(Collectors.toSet());

        List<SurveyDto> surveysToReview = new ArrayList<>();
        for (Survey survey : reviewer.getSurveys()) {
            if (survey.getArticleLinks() == null || survey.getArticleLinks().isEmpty()) {
                continue;
            }

            long reviewedCountForSurvey = 0;
            for (ArticleSurvey articleSurvey : survey.getArticleLinks()) {
                if (reviewedArticleIds.contains(articleSurvey.getArticle().getArticleId())) {
                    reviewedCountForSurvey++;
                }
            }

            if (reviewedCountForSurvey < survey.getArticleLinks().size()) {
                surveysToReview.add(toSurveyDto(survey));
            }
        }

        return surveysToReview;
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
