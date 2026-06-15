package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.AnnotationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnotationResultRepository  extends JpaRepository<AnnotationResult, Long> {
}
