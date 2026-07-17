package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.TaxonomyDimension;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxonomyValueRepository extends JpaRepository<TaxonomyValue, Long> {
    Optional<TaxonomyValue> findByDimensionAndNormalizedValue(TaxonomyDimension dimension, String normalizedValue);
}
