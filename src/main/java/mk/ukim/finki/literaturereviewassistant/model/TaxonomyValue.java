package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_taxonomy_value_dimension_normalized_value",
                columnNames = {"dimension_id", "normalized_value"}
        )
})
public class TaxonomyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taxonomyValueId;

    @Column(name = "label", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "normalized_value", nullable = false, columnDefinition = "TEXT")
    private String normalizedValue;

    @Column(columnDefinition = "TEXT")
    private String definedByJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dimension_id", nullable = false)
    private TaxonomyDimension dimension;
}
