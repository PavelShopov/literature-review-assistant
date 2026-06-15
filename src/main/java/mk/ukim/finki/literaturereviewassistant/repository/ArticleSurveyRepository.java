package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.ArticleSurvey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleSurveyRepository extends JpaRepository<ArticleSurvey, Long> {
}
