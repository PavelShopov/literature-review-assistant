package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Prompt;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptRepository extends JpaSpecificationRepository<Prompt, Long> {

}