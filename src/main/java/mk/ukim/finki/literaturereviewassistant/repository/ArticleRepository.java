package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaSpecificationRepository<Article, Long> {
    Optional<Article> findByTitleContainingIgnoreCase(String text);
    Optional<Article> findById(Long Id);


}
