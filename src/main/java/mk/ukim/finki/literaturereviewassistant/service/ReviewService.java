package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.web.dto.ReviewableArticleDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import mk.ukim.finki.literaturereviewassistant.model.Review;
import java.util.List;

public interface ReviewService {
    // Add a review for a given article within a survey
    mk.ukim.finki.literaturereviewassistant.model.Review addReview(String authorizationHeader, String surveyId, String articleId, mk.ukim.finki.literaturereviewassistant.web.dto.ReviewRequest request);
    List<SurveyDto> getSurveysToReview(String authorizationHeader);
    List<ReviewableArticleDto> getArticlesForReview(String authorizationHeader, String surveyId);
}
