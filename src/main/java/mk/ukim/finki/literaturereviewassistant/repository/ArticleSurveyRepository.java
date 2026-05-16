package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.ArticleSurvey;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleSurveyRepository extends JpaSpecificationRepository<ArticleSurvey, Long> {

}