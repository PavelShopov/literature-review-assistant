package mk.ukim.finki.literaturereviewassistant.web.controller;

import mk.ukim.finki.literaturereviewassistant.service.ReviewService;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
