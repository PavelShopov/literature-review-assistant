package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
