package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.AppUser;
import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewerRepository extends JpaRepository<Reviewer, Long> {
    List<Reviewer> findBySurveysContaining(Survey survey);

    Optional<Reviewer> findBySurveysContainingAndExternalId(Survey survey, String externalId);

    List<Reviewer> findByEmail(String email);

    List<Reviewer> findByEmailContainingIgnoreCase(String email);

    Optional<Reviewer> findByAppUser(AppUser appUser);

    Optional<Reviewer> findByAppUserAndRole(AppUser appUser, String role);
}
