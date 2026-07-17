package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_taxonomy_dimension_normalized_name", columnNames = "normalized_name")
})
public class TaxonomyDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taxonomyDimensionId;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String taxonomyDescription;

    private String generationDate;

    private Integer sourcePapersCount;

    @Column(columnDefinition = "TEXT")
    private String sourcePapersJson;

    @OneToMany(mappedBy = "dimension", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("value ASC")
    private List<TaxonomyValue> values = new ArrayList<>();
}
