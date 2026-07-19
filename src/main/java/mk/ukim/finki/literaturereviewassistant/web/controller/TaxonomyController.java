package mk.ukim.finki.literaturereviewassistant.web.controller;

import mk.ukim.finki.literaturereviewassistant.service.TaxonomyService;
import mk.ukim.finki.literaturereviewassistant.web.dto.TaxonomyDimensionDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.TaxonomyImportSummary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/taxonomies")
public class TaxonomyController {

    private final TaxonomyService taxonomyService;

    public TaxonomyController(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TaxonomyImportSummary importTaxonomy(@RequestPart(value = "file", required = false) MultipartFile file) {
        return taxonomyService.importTaxonomy(file);
    }

    @GetMapping("/dimensions")
    public List<TaxonomyDimensionDto> findDimensions() {
        return taxonomyService.findAllDimensions();
    }
}
