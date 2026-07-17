package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.ArticleSurvey;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyDimension;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyValue;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleClassificationRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleSurveyRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewerRepository;
import mk.ukim.finki.literaturereviewassistant.repository.SurveyRepository;
import mk.ukim.finki.literaturereviewassistant.repository.TaxonomyDimensionRepository;
import mk.ukim.finki.literaturereviewassistant.repository.TaxonomyValueRepository;
import mk.ukim.finki.literaturereviewassistant.service.ClassifiedArticleImportService;
import mk.ukim.finki.literaturereviewassistant.service.TaxonomyTextNormalizer;
import mk.ukim.finki.literaturereviewassistant.web.dto.ClassifiedArticleImportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:classifiedarticlestest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "gemini.api.key=",
        "nvidia.nim.api.key="
})
class ClassifiedArticleImportServiceImplTest {

    private static final String SURVEY_ID = "survey-test";

    @Autowired
    private ClassifiedArticleImportService classifiedArticleImportService;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleSurveyRepository articleSurveyRepository;

    @Autowired
    private ArticleClassificationRepository articleClassificationRepository;

    @Autowired
    private TaxonomyDimensionRepository taxonomyDimensionRepository;

    @Autowired
    private TaxonomyValueRepository taxonomyValueRepository;

    @BeforeEach
    void setUp() {
        articleClassificationRepository.deleteAll();
        articleSurveyRepository.deleteAll();
        articleRepository.deleteAll();
        taxonomyValueRepository.deleteAll();
        taxonomyDimensionRepository.deleteAll();
        reviewerRepository.deleteAll();
        surveyRepository.deleteAll();

        Survey survey = new Survey();
        survey.setExternalId(SURVEY_ID);
        survey.setTitle("Testing survey");
        survey.setDescription("Description");
        survey.setResearchQuestion("Question");
        surveyRepository.save(survey);

        TaxonomyDimension domain = dimension("Domain Type");
        value(domain, "Open domain");
        value(domain, "Closed domain");

        TaxonomyDimension paradigm = dimension("Learning Paradigm");
        value(paradigm, "Supervised");
    }

    @Test
    void importsClassifiedArticlesAndLinksTaxonomySelections() {
        ClassifiedArticleImportSummary summary = classifiedArticleImportService
                .importClassifiedArticles(SURVEY_ID, file(validArticleJson()));

        assertEquals(1, summary.getArticlesCreated());
        assertEquals(1, summary.getSurveyLinksCreated());
        assertEquals(2, summary.getClassificationsCreated());
        assertEquals(1, articleRepository.count());
        assertEquals(1, articleSurveyRepository.count());
        assertEquals(2, articleClassificationRepository.count());
    }

    @Test
    void repeatedImportDoesNotCreateDuplicateArticlesLinksOrClassifications() {
        classifiedArticleImportService.importClassifiedArticles(SURVEY_ID, file(validArticleJson()));
        ClassifiedArticleImportSummary summary = classifiedArticleImportService
                .importClassifiedArticles(SURVEY_ID, file(validArticleJson()));

        assertEquals(0, summary.getArticlesCreated());
        assertEquals(1, summary.getArticlesSkipped());
        assertEquals(0, summary.getSurveyLinksCreated());
        assertEquals(1, summary.getSurveyLinksSkipped());
        assertEquals(0, summary.getClassificationsCreated());
        assertEquals(2, summary.getClassificationsSkipped());
        assertEquals(1, articleRepository.count());
        assertEquals(1, articleSurveyRepository.count());
        assertEquals(2, articleClassificationRepository.count());
    }

    @Test
    void rejectsInvalidJson() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                classifiedArticleImportService.importClassifiedArticles(SURVEY_ID, file("{not-json"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("not valid JSON"));
    }

    @Test
    void rejectsEmptyFile() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                classifiedArticleImportService.importClassifiedArticles(
                        SURVEY_ID,
                        new MockMultipartFile("file", "classified_articles.json", "application/json", new byte[0])
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("empty"));
    }

    @Test
    void rejectsMissingRequiredRootStructure() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                classifiedArticleImportService.importClassifiedArticles(SURVEY_ID, file("""
                        {"articles":[]}
                        """))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("root must be an array"));
    }

    @Test
    void malformedClassificationSchemaIsRejectedBeforePartialWrites() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                classifiedArticleImportService.importClassifiedArticles(SURVEY_ID, file("""
                        [
                          {
                            "doi": "10.1000/valid",
                            "title": "Valid",
                            "year": "2024",
                            "classification": {
                              "classifications": [
                                {"dimension": "Domain Type", "values": ["Open domain"], "confidence": "high", "proof": "evidence"}
                              ]
                            }
                          },
                          {
                            "doi": "10.1000/broken",
                            "title": "Broken",
                            "year": "2024",
                            "classification": {}
                          }
                        ]
                        """))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("classification.classifications"));
        assertEquals(0, articleRepository.count());
        assertEquals(0, articleSurveyRepository.count());
        assertEquals(0, articleClassificationRepository.count());
    }

    @Test
    void unknownDimensionsAndValuesAreWarningsAndSkipped() {
        ClassifiedArticleImportSummary summary = classifiedArticleImportService
                .importClassifiedArticles(SURVEY_ID, file("""
                        [
                          {
                            "doi": "10.1000/unknowns",
                            "title": "Unknown taxonomy entries",
                            "year": "2024",
                            "classification": {
                              "classifications": [
                                {"dimension": "Missing Dimension", "values": ["Open domain"], "confidence": "high", "proof": "evidence"},
                                {"dimension": "Domain Type", "values": ["Missing value"], "confidence": "high", "proof": "evidence"}
                              ]
                            }
                          }
                        ]
                        """));

        assertEquals(1, summary.getArticlesCreated());
        assertEquals(0, summary.getClassificationsCreated());
        assertEquals(2, summary.getClassificationsSkipped());
        assertEquals(2, summary.getWarnings().size());
        assertTrue(summary.getWarnings().contains("1 classifications were skipped because the taxonomy dimension was unknown"));
        assertTrue(summary.getWarnings().contains("1 classification values were skipped because the taxonomy value was unknown"));
        assertEquals(0, articleClassificationRepository.count());
    }

    @Test
    void nullAndEmptyValuesAreWarningsAndDoNotCreatePlaceholderValues() {
        ClassifiedArticleImportSummary summary = classifiedArticleImportService
                .importClassifiedArticles(SURVEY_ID, file("""
                        [
                          {
                            "doi": "10.1000/null-values",
                            "title": "Null values",
                            "year": "2024",
                            "classification": {
                              "classifications": [
                                {"dimension": "Domain Type", "values": null, "confidence": "medium", "proof": "none"},
                                {"dimension": "Learning Paradigm", "values": [], "confidence": "medium", "proof": "none"}
                              ]
                            }
                          }
                        ]
                        """));

        assertEquals(0, summary.getClassificationsCreated());
        assertEquals(2, summary.getClassificationsSkipped());
        assertEquals(1, summary.getWarnings().size());
        assertTrue(summary.getWarnings().contains("2 classifications were skipped because they had no taxonomy values"));
        assertEquals(3, taxonomyValueRepository.count());
        assertEquals(0, articleClassificationRepository.count());
    }

    @Test
    void importedBlankValuesDoNotOverwriteExistingArticleData() {
        Article existing = new Article();
        existing.setExternalId("article-existing");
        existing.setDoi("10.1000/existing");
        existing.setTitle("Existing rich title");
        existing.setPublicationYear(2020);
        existing.setJournal("Existing Journal");
        existing.setStatus("INCLUDED");
        articleRepository.save(existing);

        classifiedArticleImportService.importClassifiedArticles(SURVEY_ID, file("""
                [
                  {
                    "doi": "10.1000/existing",
                    "title": "   ",
                    "year": "",
                    "classification": {
                      "classifications": [
                        {"dimension": "Domain Type", "values": ["Open domain"], "confidence": "high", "proof": "evidence"}
                      ]
                    }
                  }
                ]
                """));

        Article saved = articleRepository.findByDoi("10.1000/existing").orElseThrow();
        assertEquals("Existing rich title", saved.getTitle());
        assertEquals(2020, saved.getPublicationYear());
        assertEquals("Existing Journal", saved.getJournal());
        assertEquals("INCLUDED", saved.getStatus());
    }

    private TaxonomyDimension dimension(String name) {
        TaxonomyDimension dimension = new TaxonomyDimension();
        dimension.setName(name);
        dimension.setNormalizedName(TaxonomyTextNormalizer.normalize(name));
        dimension.setDescription(name + " description");
        return taxonomyDimensionRepository.save(dimension);
    }

    private TaxonomyValue value(TaxonomyDimension dimension, String value) {
        TaxonomyValue taxonomyValue = new TaxonomyValue();
        taxonomyValue.setDimension(dimension);
        taxonomyValue.setValue(value);
        taxonomyValue.setNormalizedValue(TaxonomyTextNormalizer.normalize(value));
        return taxonomyValueRepository.save(taxonomyValue);
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile(
                "file",
                "classified_articles.json",
                "application/json",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String validArticleJson() {
        return """
                [
                  {
                    "doi": "https://doi.org/10.1000/example",
                    "title": "Example classified article",
                    "year": "2024",
                    "matched_categories": ["Domain Type"],
                    "text_source": "abstract",
                    "classification": {
                      "classifications": [
                        {"dimension": "Domain Type", "values": ["Open domain"], "confidence": "high", "proof": "domain evidence"},
                        {"dimension": "Learning Paradigm", "values": ["Supervised"], "confidence": "medium", "proof": "learning evidence"}
                      ]
                    }
                  }
                ]
                """;
    }
}
