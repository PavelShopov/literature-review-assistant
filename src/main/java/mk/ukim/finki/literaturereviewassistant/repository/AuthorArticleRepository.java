package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.AuthorArticle;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorArticleRepository extends JpaSpecificationRepository<AuthorArticle, Long> {

}