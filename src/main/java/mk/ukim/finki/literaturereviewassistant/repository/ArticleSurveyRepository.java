package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.ArticleSurvey;
import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleSurveyRepository extends JpaRepository<ArticleSurvey, Long> {
    Optional<ArticleSurvey> findBySurveyAndArticle(Survey survey, Article article);
}
