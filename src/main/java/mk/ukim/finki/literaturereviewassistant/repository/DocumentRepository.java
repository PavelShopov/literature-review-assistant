package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.Document;
import mk.ukim.finki.literaturereviewassistant.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findFirstByArticleExternalIdAndTypeAndFilePathIsNotNull(String articleId, DocumentType type);
    Optional<Document> findFirstByArticleExternalIdAndTypeAndPdfContentIsNotNull(String articleId, DocumentType type);

    Optional<Document> findFirstByChecksumSha256(String checksumSha256);
}
