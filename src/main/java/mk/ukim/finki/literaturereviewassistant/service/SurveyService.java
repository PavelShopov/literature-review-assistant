package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.AddedByDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleImportRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleUpdateRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.PdfDownload;
import mk.ukim.finki.literaturereviewassistant.web.dto.ReviewerDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDetailsDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyAskRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SurveyService {
    List<SurveyDto> findAllSurveys(String authorizationHeader);

    SurveyDetailsDto findSurvey(String surveyId);

    SurveyDto saveSurvey(String surveyId, SurveyRequest request, String authorizationHeader);

    void deleteSurvey(String surveyId);

    List<ArticleDto> findArticles(String surveyId);

    ArticleDto findArticle(String articleId);

    PdfDownload findArticlePdf(String articleId, String authorizationHeader);

    ArticleDto importArticle(String surveyId, ArticleImportRequest request, String authorizationHeader);

    ArticleDto importPdfArticle(String surveyId, MultipartFile file, AddedByDto addedBy);

    ArticleDto updateArticle(String surveyId, String articleId, ArticleUpdateRequest request, String authorizationHeader);

    void deleteArticle(String surveyId, String articleId, String authorizationHeader);

    String askSurvey(String surveyId, SurveyAskRequest request);

    List<ReviewerDto> findReviewers(String surveyId);

    ReviewerDto addReviewer(String surveyId, ReviewerDto reviewer, String authorizationHeader);

    void removeReviewer(String surveyId, String reviewerId, String authorizationHeader);

    List<ReviewerDto> searchReviewersByEmail(String email);

    ReviewerDto toReviewerDto(Reviewer reviewer);

    ArticleDto toArticleDto(Article article);
}
