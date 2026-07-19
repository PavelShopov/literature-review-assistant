package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.web.dto.TaxonomyDimensionDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.TaxonomyImportSummary;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaxonomyService {
    TaxonomyImportSummary importTaxonomy(MultipartFile file);

    List<TaxonomyDimensionDto> findAllDimensions();
}
