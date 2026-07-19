package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.model.TaxonomyDimension;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyValue;
import mk.ukim.finki.literaturereviewassistant.repository.TaxonomyDimensionRepository;
import mk.ukim.finki.literaturereviewassistant.repository.TaxonomyValueRepository;
import mk.ukim.finki.literaturereviewassistant.service.TaxonomyService;
import mk.ukim.finki.literaturereviewassistant.web.dto.TaxonomyImportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:taxonomytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "gemini.api.key=",
        "nvidia.nim.api.key="
})
class TaxonomyServiceImplTest {

    @Autowired
    private TaxonomyService taxonomyService;

    @Autowired
    private TaxonomyDimensionRepository dimensionRepository;

    @Autowired
    private TaxonomyValueRepository valueRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        valueRepository.deleteAll();
        dimensionRepository.deleteAll();
    }

    @Test
    void importsValidTaxonomy() {
        TaxonomyImportSummary summary = taxonomyService.importTaxonomy(file(validTaxonomy()));

        assertEquals(2, summary.getDimensionsCreated());
        assertEquals(3, summary.getValuesCreated());
        assertEquals(2, dimensionRepository.count());
        assertEquals(3, valueRepository.count());
        assertEquals("Domain Type", summary.getDimensions().get(0).name());
        assertEquals("Open domain", summary.getDimensions().get(0).options().get(0));
    }

    @Test
    void repeatedImportDoesNotCreateDuplicates() {
        taxonomyService.importTaxonomy(file(validTaxonomy()));
        TaxonomyImportSummary summary = taxonomyService.importTaxonomy(file(validTaxonomy()));

        assertEquals(0, summary.getDimensionsCreated());
        assertEquals(0, summary.getValuesCreated());
        assertEquals(2, summary.getDimensionsSkipped());
        assertEquals(3, summary.getValuesSkipped());
        assertEquals(2, dimensionRepository.count());
        assertEquals(3, valueRepository.count());
    }

    @Test
    void rejectsInvalidJson() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                taxonomyService.importTaxonomy(file("{not-json"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("not valid JSON"));
    }

    @Test
    void rejectsEmptyFile() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                taxonomyService.importTaxonomy(new MockMultipartFile("file", "taxonomy.json", "application/json", new byte[0]))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("empty"));
    }

    @Test
    void rejectsMissingRequiredRootStructure() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                taxonomyService.importTaxonomy(file("""
                        {"dimensions":[]}
                        """))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("unified_taxonomy_of_dimensions"));
    }

    @Test
    void rejectsMalformedDimension() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                taxonomyService.importTaxonomy(file("""
                        {
                          "unified_taxonomy_of_dimensions": {
                            "dimensions": [
                              {"category": " ", "description": "desc", "possible_values": []}
                            ]
                          }
                        }
                        """))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("category"));
    }

    @Test
    void duplicateValuesInUploadedDimensionAreSkippedWithWarning() {
        TaxonomyImportSummary summary = taxonomyService.importTaxonomy(file("""
                {
                  "unified_taxonomy_of_dimensions": {
                    "description": "Taxonomy",
                    "generation_date": "2026-03-18",
                    "source_papers_count": 1,
                    "source_papers": ["paper.json"],
                    "dimensions": [
                      {
                        "category": "Domain Type",
                        "description": "Domain description",
                        "possible_values": [
                          {"value": "Open domain", "defined_by": ["paper"]},
                          {"value": "  open   domain  ", "defined_by": ["paper"]}
                        ]
                      }
                    ]
                  }
                }
                """));

        assertEquals(1, summary.getValuesCreated());
        assertEquals(1, summary.getValuesSkipped());
        assertFalse(summary.getWarnings().isEmpty());
        assertEquals(1, valueRepository.count());
    }

    @Test
    void rollsBackCreatedRecordsWhenLaterValueIsMalformed() {
        assertThrows(ResponseStatusException.class, () ->
                transactionTemplate.executeWithoutResult(status ->
                        taxonomyService.importTaxonomy(file("""
                                {
                                  "unified_taxonomy_of_dimensions": {
                                    "description": "Taxonomy",
                                    "generation_date": "2026-03-18",
                                    "source_papers_count": 1,
                                    "source_papers": ["paper.json"],
                                    "dimensions": [
                                      {
                                        "category": "Domain Type",
                                        "description": "Domain description",
                                        "possible_values": [
                                          {"value": "Open domain", "defined_by": ["paper"]}
                                        ]
                                      },
                                      {
                                        "category": "Broken",
                                        "description": "Broken description",
                                        "possible_values": [
                                          {"value": "", "defined_by": ["paper"]}
                                        ]
                                      }
                                    ]
                                  }
                                }
                                """))
                )
        );

        assertEquals(0, dimensionRepository.count());
        assertEquals(0, valueRepository.count());
    }

    @Test
    void updatesExistingDescriptionsAndValueMetadata() {
        taxonomyService.importTaxonomy(file(validTaxonomy()));
        TaxonomyImportSummary summary = taxonomyService.importTaxonomy(file("""
                {
                  "unified_taxonomy_of_dimensions": {
                    "description": "Updated taxonomy",
                    "generation_date": "2026-03-19",
                    "source_papers_count": 2,
                    "source_papers": ["paper-a.json", "paper-b.json"],
                    "dimensions": [
                      {
                        "category": "Domain Type",
                        "description": "Updated domain description",
                        "possible_values": [
                          {"value": "Open domain", "defined_by": ["paper-a", "paper-b"]},
                          {"value": "Closed domain", "defined_by": ["paper-a"]}
                        ]
                      }
                    ]
                  }
                }
                """));

        assertEquals(1, summary.getDimensionsUpdated());
        assertEquals(2, summary.getValuesUpdated());
        TaxonomyDimension dimension = dimensionRepository.findByNormalizedName("domain type").orElseThrow();
        TaxonomyValue value = valueRepository.findByDimensionAndNormalizedValue(dimension, "open domain").orElseThrow();
        assertEquals("Updated domain description", dimension.getDescription());
        assertTrue(value.getDefinedByJson().contains("paper-b"));
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile(
                "file",
                "taxonomy.json",
                "application/json",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String validTaxonomy() {
        return """
                {
                  "unified_taxonomy_of_dimensions": {
                    "description": "Taxonomy",
                    "generation_date": "2026-03-18",
                    "source_papers_count": 2,
                    "source_papers": ["paper-a.json", "paper-b.json"],
                    "dimensions": [
                      {
                        "category": "Domain Type",
                        "description": "Domain description",
                        "possible_values": [
                          {"value": "Open domain", "defined_by": ["paper-a"]},
                          {"value": "Closed domain", "defined_by": ["paper-b"]}
                        ]
                      },
                      {
                        "category": "Knowledge Source Type",
                        "description": "Source description",
                        "possible_values": [
                          {"value": "Structured data", "defined_by": ["paper-a"]}
                        ]
                      }
                    ]
                  }
                }
                """;
    }
}
