package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.model.*;
import mk.ukim.finki.literaturereviewassistant.repository.*;
import mk.ukim.finki.literaturereviewassistant.web.dto.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:surveytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "gemini.api.key=",
        "nvidia.nim.api.key="
})
@Transactional
class SurveyServiceImplImportTest {
    @Autowired
    private SurveyServiceImpl service;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    @Autowired
    private RestTemplate restTemplate;

    private Survey survey;
    private Path storageDir;
    private MockRestServiceServer restServer;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(service, "maxPdfSize", 10_000_000L);

        storageDir = Files.createTempDirectory("paper-storage-test");
        ReflectionTestUtils.setField(service, "paperStorageDir", storageDir.toString());

        restServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();

        articleRepository.deleteAll();
        documentRepository.deleteAll();
        authorRepository.deleteAll();
        authSessionRepository.deleteAll();
        reviewerRepository.deleteAll();
        surveyRepository.deleteAll();

        survey = new Survey();
        survey.setExternalId("survey-1");
        survey.setTitle("Test survey");
        survey.setDescription("Description");
        survey.setResearchQuestion("Question");
        survey.setStatus("Draft");
        surveyRepository.save(survey);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (storageDir != null && Files.exists(storageDir)) {
            try (var paths = Files.walk(storageDir)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    @Test
    void normalizesSupportedDoiForms() {
        assertEquals("10.1000/example", service.normalizeDoi(" DOI:10.1000/Example "));
        assertEquals("10.1000/example", service.normalizeDoi("https://doi.org/10.1000/Example"));
        assertEquals("10.1000/example", service.normalizeDoi("http://dx.doi.org/10.1000/Example"));
    }

    @Test
    void importsDoiOnlyAndKeepsNormalizedDoiWhenProviderIsUnavailable() {
        String doi = "10.1000/example";
        restServer.expect(requestTo(Matchers.containsString("api.crossref.org/works/" + doi)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"message": {}}
                        """, MediaType.APPLICATION_JSON));
        restServer.expect(ExpectedCount.times(2), requestTo(Matchers.containsString("doi.org/" + doi)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    String accept = request.getHeaders().getFirst(HttpHeaders.ACCEPT);
                    if ("application/vnd.citationstyles.csl+json".equals(accept)) {
                        return withSuccess("""
                                {}
                                """, MediaType.APPLICATION_JSON).createResponse(request);
                    }
                    return withSuccess("""
                            <html><head><title>Example article</title></head><body></body></html>
                            """, MediaType.TEXT_HTML).createResponse(request);
                });

        ArticleDto result = service.importArticle(
                "survey-1",
                new ArticleImportRequest("url", "https://doi.org/10.1000/Example", addedBy(), null, null),
                null
        );

        restServer.verify();
        assertEquals("10.1000/example", result.doi());
        assertEquals("https://doi.org/10.1000/example", result.openUrl());
        assertEquals("doi", result.openType());
    }

    @Test
    void importsTitleAuthorsAndPublicationMetadataFromCrossref() {
        String doi = "10.3390/app9245574";
        restServer.expect(requestTo(Matchers.containsString("api.crossref.org/works/" + doi)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "message": {
                            "DOI": "10.3390/app9245574",
                            "title": ["Machine Learning for Quantitative Finance Applications: A Survey"],
                            "container-title": ["Applied Sciences"],
                            "URL": "https://doi.org/10.3390/app9245574",
                            "author": [
                              {"given": "Francesco", "family": "Rundo"},
                              {"given": "Francesca", "family": "Trenta"}
                            ],
                            "published-online": {"date-parts": [[2019, 12, 17]]}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        restServer.expect(ExpectedCount.times(2), requestTo(Matchers.containsString("doi.org/" + doi)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    String accept = request.getHeaders().getFirst(HttpHeaders.ACCEPT);
                    if ("application/vnd.citationstyles.csl+json".equals(accept)) {
                        return withSuccess("""
                                {}
                                """, MediaType.APPLICATION_JSON).createResponse(request);
                    }
                    return withSuccess("""
                            <html><head><title>Machine Learning for Quantitative Finance Applications: A Survey</title></head><body></body></html>
                            """, MediaType.TEXT_HTML).createResponse(request);
                });

        ArticleDto result = service.importArticle(
                "survey-1",
                new ArticleImportRequest("url", doi, addedBy(), null, null),
                null
        );

        restServer.verify();
        assertEquals("Machine Learning for Quantitative Finance Applications: A Survey", result.title());
        assertEquals(List.of("Rundo, Francesco", "Trenta, Francesca"), result.authors());
        assertEquals("Applied Sciences", result.journal());
        assertEquals(2019, result.year());
        assertEquals(doi, result.doi());
    }

    @Test
    void storesPdfBytesAndReturnsPdfAsCanonicalOpenTarget() throws Exception {
        byte[] pdf = validPdf();
        ArticleDto result = service.importArticle(
                "survey-1",
                new ArticleImportRequest(
                        "pdf",
                        Base64.getEncoder().encodeToString(pdf),
                        addedBy(),
                        "../unsafe name.pdf",
                        MediaType.APPLICATION_PDF_VALUE
                ),
                null
        );

        assertTrue(result.hasPdf());
        assertEquals("pdf", result.openType());
        assertTrue(result.openUrl().endsWith("/pdf"));

        Article saved = articleRepository.findByExternalId(result.id()).orElseThrow();
        Document document = saved.getDocuments().get(0);
        assertNotNull(document.getFilePath());
        assertTrue(Files.exists(storageDir.resolve(document.getFilePath())));
        assertNull(document.getPdfContent());
        assertFalse(document.getOriginalFileName().contains(".."));
        assertNotNull(document.getChecksumSha256());
    }

    @Test
    void returnsExistingArticleForDuplicateDoi() {
        Article existing = article("article-existing", "10.1000/example");
        existing.setTitle("Imported article for 10 1000 example");
        existing.setPublicationYear(java.time.LocalDate.now().getYear());
        articleRepository.save(existing);

        ArticleDto result = service.importArticle(
                "survey-1",
                new ArticleImportRequest(
                        "bibtex",
                        "@article{x,title={Existing},doi={DOI:10.1000/Example},year={2024}}",
                        addedBy(),
                        null,
                        null
                ),
                null
        );

        assertEquals("article-existing", result.id());
        assertEquals(1, articleRepository.findByExternalId("article-existing").orElseThrow().getSurveyLinks().size());
        assertEquals("Existing", result.title());
        assertEquals(2024, result.year());
    }

    @Test
    void returnsExistingArticleForDuplicatePdfChecksum() throws Exception {
        byte[] pdf = validPdf();
        Article existing = article("article-existing", null);
        articleRepository.save(existing);

        Document existingDocument = new Document();
        existingDocument.setArticle(existing);
        existingDocument.setType(DocumentType.PDF);
        existingDocument.setChecksumSha256(hexSha256(pdf));
        documentRepository.save(existingDocument);

        ArticleDto result = service.importArticle(
                "survey-1",
                new ArticleImportRequest(
                        "pdf",
                        Base64.getEncoder().encodeToString(pdf),
                        addedBy(),
                        "paper.pdf",
                        MediaType.APPLICATION_PDF_VALUE
                ),
                null
        );

        assertEquals("article-existing", result.id());
    }

    @Test
    void selectsPdfThenExternalThenDoiForOpenUrl() {
        Article doi = article("doi", "10.1000/example");
        assertEquals("doi", service.toArticleDto(doi).openType());

        doi.setUrl("https://publisher.example/paper");
        assertEquals("external", service.toArticleDto(doi).openType());

        Document pdf = new Document();
        pdf.setType(DocumentType.PDF);
        pdf.setFilePath("paper.pdf");
        doi.getDocuments().add(pdf);
        assertEquals("pdf", service.toArticleDto(doi).openType());
    }

    @Test
    void usesOnlineScholarlySearchWhenBibTexHasNoDoiOrUrl() {
        Article article = article("title-only", null);
        article.setTitle("Machine learning a primer");

        ArticleDto dto = service.toArticleDto(article);

        assertEquals("external", dto.openType());
        assertTrue(dto.openUrl().startsWith("https://scholar.google.com/scholar?q="));
    }

    @Test
    void rejectsInvalidPdf() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                service.importArticle(
                        "survey-1",
                        new ArticleImportRequest(
                                "pdf",
                                Base64.getEncoder().encodeToString("not a pdf".getBytes()),
                                addedBy(),
                                "fake.pdf",
                                MediaType.APPLICATION_PDF_VALUE
                        ),
                        null
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void pdfDownloadRequiresAuthenticationAndReturnsStoredBytesForAdmin() throws Exception {
        Article article = article("article-1", null);
        articleRepository.save(article);

        assertThrows(ResponseStatusException.class, () -> service.findArticlePdf("article-1", null));

        byte[] bytes = "%PDF-test".getBytes();
        Document document = new Document();
        document.setFilePath("legacy.pdf");
        Files.write(storageDir.resolve("legacy.pdf"), bytes);
        document.setType(DocumentType.PDF);
        document.setMimeType(MediaType.APPLICATION_PDF_VALUE);
        document.setOriginalFileName("paper.pdf");
        document.setArticle(article);
        documentRepository.save(document);

        AppUser admin = new AppUser(null, "Admin", "admin@example.com", "hash", "ADMIN");
        admin = appUserRepository.save(admin);
        AuthSession session = new AuthSession(null, "token", Instant.now(), admin);
        authSessionRepository.save(session);

        PdfDownload download = service.findArticlePdf("article-1", "Bearer token");
        assertArrayEquals(bytes, download.content());
        assertEquals("paper.pdf", download.fileName());
    }

    @Test
    void createsSurveyAndMakesItVisibleToTheOwner() {
        AppUser owner = appUserRepository.save(new AppUser(null, "Owner", "owner@test.com", "hash", "USER"));
        authSessionRepository.save(new AuthSession(null, "owner-token", Instant.now(), owner));

        SurveyDto created = service.saveSurvey(
                "survey-temp-1",
                new SurveyRequest(
                        "survey-temp-1",
                        "Created survey",
                        "Created from test",
                        null,
                        "Draft",
                        0,
                        "Research question"
                ),
                "Bearer owner-token"
        );

        assertNotNull(created.id());
        assertEquals("Created survey", created.name());

        List<SurveyDto> surveys = service.findAllSurveys("Bearer owner-token");
        assertTrue(surveys.stream().anyMatch(survey -> survey.id().equals(created.id())));
    }

    @Test
    void updatesStatusForArticleLinkedThroughSurveyJoinTable() {
        AppUser owner = appUserRepository.save(new AppUser(null, "Owner", "owner@test.com", "hash", "USER"));
        authSessionRepository.save(new AuthSession(null, "owner-token", Instant.now(), owner));

        Survey savedSurvey = surveyRepository.findByExternalId("survey-1").orElseThrow();
        Article article = article("article-linked", null);
        article.setSurveyExternalId(null);
        articleRepository.save(article);

        ArticleSurvey link = new ArticleSurvey();
        link.setArticle(article);
        link.setSurvey(savedSurvey);
        link.setStatus(ArticleStatus.PENDING);
        article.getSurveyLinks().add(link);
        savedSurvey.getArticleLinks().add(link);
        articleRepository.save(article);

        ArticleDto updated = service.updateArticle(
                savedSurvey.getExternalId(),
                article.getExternalId(),
                new ArticleUpdateRequest(
                        article.getTitle(),
                        List.of("Author One"),
                        article.getJournal(),
                        article.getPublicationYear(),
                        article.getDoi(),
                        "INCLUDED",
                        article.getArticleAbstract(),
                        article.getInclusionSummary()
                ),
                "Bearer owner-token"
        );

        assertEquals("INCLUDED", updated.status());
    }

    private AddedByDto addedBy() {
        return new AddedByDto("1", "Owner", "Owner");
    }

    private Article article(String id, String doi) {
        Article article = new Article();
        article.setExternalId(id);
        article.setTitle("Paper");
        article.setDoi(doi);
        article.setPublicationYear(2024);
        article.setStatus("PENDING");
        article.setAuthors(new ArrayList<>());
        article.setDocuments(new ArrayList<>());
        article.setSurveyLinks(new ArrayList<>());
        return article;
    }

    private byte[] validPdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private String hexSha256(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
