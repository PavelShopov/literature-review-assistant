package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaSpecificationRepository<Article, Long> {
    Optional<Article> findByTitleContainingIgnoreCase(String text);
    Page<Article> findByArticleAbstractContainingIgnoreCase(String articleAbstract, Pageable pageable);

    Optional<Article> findByDoi(String doi);

    Optional<Article> findByUrl(String url);

    List<Article> findBySurveysContaining(Survey survey);
}
