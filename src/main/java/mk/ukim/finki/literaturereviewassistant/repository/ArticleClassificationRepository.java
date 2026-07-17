package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.ArticleClassification;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyDimension;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleClassificationRepository extends JpaRepository<ArticleClassification, Long> {
    Optional<ArticleClassification> findByArticleAndTaxonomyDimensionAndTaxonomyValue(
            Article article,
            TaxonomyDimension taxonomyDimension,
            TaxonomyValue taxonomyValue
    );

    long countByArticle(Article article);
}
