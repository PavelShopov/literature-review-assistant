package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Review;
import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByArticle(Article article);

    @Query("SELECT r FROM Review r WHERE r.Reviewer = :reviewer")
    List<Review> findByReviewer(@Param("reviewer") Reviewer reviewer);
}
