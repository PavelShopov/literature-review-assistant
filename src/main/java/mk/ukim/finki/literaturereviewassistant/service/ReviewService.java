package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;

import java.util.List;

public interface ReviewService {
    List<SurveyDto> getSurveysToReview(String authorizationHeader);
}
