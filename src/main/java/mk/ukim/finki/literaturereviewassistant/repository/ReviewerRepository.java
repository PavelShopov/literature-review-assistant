package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewerRepository extends JpaRepository<Reviewer, Long> {
    List<Reviewer> findBySurveysContaining(Survey survey);

    Optional<Reviewer> findBySurveysContainingAndExternalId(Survey survey, String externalId);

    Optional<Reviewer> findByEmail(String email);

    Optional<Reviewer> findFirstByEmail(String email);

    List<Reviewer> findByEmailContainingIgnoreCase(String email);
}
