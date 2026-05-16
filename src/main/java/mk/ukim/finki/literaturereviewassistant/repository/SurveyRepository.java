package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Survey;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyRepository extends JpaSpecificationRepository<Survey, Long> {

}