package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.TaxonomyDimension;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxonomyDimensionRepository extends JpaRepository<TaxonomyDimension, Long> {
    Optional<TaxonomyDimension> findByNormalizedName(String normalizedName);

    @EntityGraph(attributePaths = "values")
    List<TaxonomyDimension> findAllByOrderByNameAsc();
}
