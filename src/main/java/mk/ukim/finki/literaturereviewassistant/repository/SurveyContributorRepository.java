package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.SurveyContributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyContributorRepository extends JpaRepository<SurveyContributor, Long> {
    List<SurveyContributor> findBySurvey_SurveyId(Long surveyId);
    List<SurveyContributor> findByUser_Id(Long userId);
    void deleteBySurvey_SurveyIdAndUser_Id(Long surveyId, Long userId);
}
