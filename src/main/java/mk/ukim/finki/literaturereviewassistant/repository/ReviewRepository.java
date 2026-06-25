package mk.ukim.finki.literaturereviewassistant.repository;


import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Review;
import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByReviewer(Reviewer reviewer);
    Optional<Review> findByReviewerAndArticle(Reviewer reviewer, Article article);
    Optional<Review> findTopByArticle_ArticleIdOrderByReviewIdDesc(Long articleId);
    Optional<Review> findByArticleAndReviewer(Article article, Reviewer reviewer);
}
