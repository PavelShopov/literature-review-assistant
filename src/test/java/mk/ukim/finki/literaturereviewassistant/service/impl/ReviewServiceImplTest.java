package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.model.*;
import mk.ukim.finki.literaturereviewassistant.repository.*;
import mk.ukim.finki.literaturereviewassistant.web.dto.ReviewRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:reviewtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Transactional
class ReviewServiceImplTest {

    @Autowired
    private ReviewServiceImpl reviewService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void approveReviewUpdatesArticleAndStoresReview() {
        AppUser reviewerUser = appUserRepository.save(new AppUser(null, "Jana Reviewer", "jana@example.com", "hash", "USER"));
        authSessionRepository.save(new AuthSession(null, "review-token", Instant.now(), reviewerUser));

        Survey survey = new Survey();
        survey.setExternalId("survey-review-1");
        survey.setTitle("Survey");
        survey.setDescription("Desc");
        survey.setResearchQuestion("RQ");
        survey.setCreatedDate(LocalDate.now());
        survey.setStatus("ACTIVE");
        survey = surveyRepository.save(survey);

        Reviewer reviewer = new Reviewer();
        reviewer.setExternalId("reviewer-review-1");
        reviewer.setName("Jana Reviewer");
        reviewer.setEmail("jana@example.com");
        reviewer.setRole("Reviewer");
        reviewer.setAddedDate(Instant.now());
        reviewer.setAppUser(reviewerUser);
        reviewer.setSurveys(List.of(survey));
        reviewerRepository.save(reviewer);

        Article article = new Article();
        article.setExternalId("article-review-1");
        article.setSurveyExternalId(survey.getExternalId());
        article.setTitle("Paper");
        article.setJournal("Journal");
        article.setPublicationYear(2025);
        article.setStatus("PENDING");
        article.setAddedById("jana");
        article.setAddedByName("Jana");
        article.setAddedByRole("Reviewer");

        ArticleSurvey link = new ArticleSurvey();
        link.setArticle(article);
        link.setSurvey(survey);
        link.setStatus(ArticleStatus.PENDING);
        article.getSurveyLinks().add(link);
        survey.getArticleLinks().add(link);
        articleRepository.save(article);

        reviewService.addReview(
                "Bearer review-token",
                survey.getExternalId(),
                article.getExternalId(),
                reviewPayload("approve", "Looks good")
        );

        Article updatedArticle = articleRepository.findByExternalId(article.getExternalId()).orElseThrow();
        assertEquals("INCLUDED", updatedArticle.getStatus());
        assertEquals(ArticleStatus.INCLUDED, updatedArticle.getSurveyLinks().get(0).getStatus());
        assertEquals(1, reviewRepository.findByReviewerAndArticle(reviewer, updatedArticle).stream().count());
    }

    @Test
    void denyReviewUpdatesArticleToExcluded() {
        AppUser reviewerUser = appUserRepository.save(new AppUser(null, "Jana Reviewer", "jana2@example.com", "hash", "USER"));
        authSessionRepository.save(new AuthSession(null, "review-token-2", Instant.now(), reviewerUser));

        Survey survey = new Survey();
        survey.setExternalId("survey-review-2");
        survey.setTitle("Survey");
        survey.setDescription("Desc");
        survey.setResearchQuestion("RQ");
        survey.setCreatedDate(LocalDate.now());
        survey.setStatus("ACTIVE");
        survey = surveyRepository.save(survey);

        Reviewer reviewer = new Reviewer();
        reviewer.setExternalId("reviewer-review-2");
        reviewer.setName("Jana Reviewer");
        reviewer.setEmail("jana2@example.com");
        reviewer.setRole("Reviewer");
        reviewer.setAddedDate(Instant.now());
        reviewer.setAppUser(reviewerUser);
        reviewer.setSurveys(List.of(survey));
        reviewerRepository.save(reviewer);

        Article article = new Article();
        article.setExternalId("article-review-2");
        article.setSurveyExternalId(survey.getExternalId());
        article.setTitle("Paper");
        article.setJournal("Journal");
        article.setPublicationYear(2025);
        article.setStatus("PENDING");
        article.setAddedById("jana");
        article.setAddedByName("Jana");
        article.setAddedByRole("Reviewer");

        ArticleSurvey link = new ArticleSurvey();
        link.setArticle(article);
        link.setSurvey(survey);
        link.setStatus(ArticleStatus.PENDING);
        article.getSurveyLinks().add(link);
        survey.getArticleLinks().add(link);
        articleRepository.save(article);

        reviewService.addReview(
                "Bearer review-token-2",
                survey.getExternalId(),
                article.getExternalId(),
                reviewPayload("deny", "Not relevant")
        );

        Article updatedArticle = articleRepository.findByExternalId(article.getExternalId()).orElseThrow();
        assertEquals("EXCLUDED", updatedArticle.getStatus());
        assertEquals(ArticleStatus.EXCLUDED, updatedArticle.getSurveyLinks().get(0).getStatus());
    }

    private ReviewRequest reviewPayload(String decision, String note) {
        ReviewRequest request = new ReviewRequest();
        request.setJsonResponse("""
                {"decision":"%s","note":"%s","timestamp":"2026-06-25T00:00:00Z"}
                """.formatted(decision, note));
        return request;
    }
}
