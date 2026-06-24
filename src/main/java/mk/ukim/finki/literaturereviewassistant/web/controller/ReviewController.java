package mk.ukim.finki.literaturereviewassistant.web.controller;

import mk.ukim.finki.literaturereviewassistant.service.ReviewService;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/surveys")
    public List<SurveyDto> getSurveysToReview(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return reviewService.getSurveysToReview(authorizationHeader);
    }

    @GetMapping("/surveys/{surveyId}/articles")
    public List<mk.ukim.finki.literaturereviewassistant.web.dto.ReviewableArticleDto> getArticlesForReview(
            @PathVariable String surveyId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return reviewService.getArticlesForReview(authorizationHeader, surveyId);
    }

    @PostMapping("/surveys/{surveyId}/articles/{articleId}")
    public mk.ukim.finki.literaturereviewassistant.model.Review addReview(
            @PathVariable String surveyId,
            @PathVariable String articleId,
            @RequestBody mk.ukim.finki.literaturereviewassistant.web.dto.ReviewRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return reviewService.addReview(authorizationHeader, surveyId, articleId, request);
    }
}
