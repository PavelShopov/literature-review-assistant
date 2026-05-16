package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaSpecificationRepository<Article, Long> {

}
