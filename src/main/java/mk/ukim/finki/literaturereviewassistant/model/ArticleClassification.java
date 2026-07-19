package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_article_classification_article_dimension_value",
                columnNames = {"article_id", "taxonomy_dimension_id", "taxonomy_value_id"}
        )
})
public class ArticleClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleClassificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxonomy_dimension_id", nullable = false)
    private TaxonomyDimension taxonomyDimension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxonomy_value_id", nullable = false)
    private TaxonomyValue taxonomyValue;

    private String confidence;

    @Column(columnDefinition = "TEXT")
    private String proof;

    private Instant importedAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        importedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
