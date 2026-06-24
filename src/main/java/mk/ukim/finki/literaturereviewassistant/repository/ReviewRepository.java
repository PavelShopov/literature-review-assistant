package mk.ukim.finki.literaturereviewassistant.repository;


import mk.ukim.finki.literaturereviewassistant.model.Review;
import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByReviewer(Reviewer reviewer);
}
