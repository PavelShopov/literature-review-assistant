package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.model.*;
import mk.ukim.finki.literaturereviewassistant.repository.*;
import mk.ukim.finki.literaturereviewassistant.service.DataService.BibTexParser;
import mk.ukim.finki.literaturereviewassistant.service.DataService.PdfExtractorService;
import mk.ukim.finki.literaturereviewassistant.service.OllamaService;
import mk.ukim.finki.literaturereviewassistant.web.dto.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SurveyServiceImplImportTest {
    private SurveyRepository surveyRepository;
    private ArticleRepository articleRepository;
    private AuthorRepository authorRepository;
    private AuthSessionRepository authSessionRepository;
    private DocumentRepository documentRepository;
    private RestTemplate restTemplate;
    private SurveyServiceImpl service;
    private Survey survey;
    private Path storageDir;

    @BeforeEach
    void setUp() throws Exception {
        surveyRepository = mock(SurveyRepository.class);
        articleRepository = mock(ArticleRepository.class);
        authorRepository = mock(AuthorRepository.class);
        authSessionRepository = mock(AuthSessionRepository.class);
        documentRepository = mock(DocumentRepository.class);
        ReviewerRepository reviewerRepository = mock(ReviewerRepository.class);
        restTemplate = mock(RestTemplate.class);

        service = new SurveyServiceImpl(
                surveyRepository,
                articleRepository,
                authorRepository,
                authSessionRepository,
                documentRepository,
                reviewerRepository,
                new BibTexParser(),
                new PdfExtractorService(),
                mock(OllamaService.class),
                restTemplate
        );
        ReflectionTestUtils.setField(service, "maxPdfSize", 10_000_000L);
        storageDir = Files.createTempDirectory("paper-storage-test");
        ReflectionTestUtils.setField(service, "paperStorageDir", storageDir.toString());

        survey = new Survey();
        survey.setSurveyId(1L);
        survey.setExternalId("survey-1");
        when(surveyRepository.findByExternalId("survey-1")).thenReturn(Optional.of(survey));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(articleRepository.findByDoi(anyString())).thenReturn(Optional.empty());
        when(articleRepository.findFirstByTitleIgnoreCaseAndPublicationYear(anyString(), anyInt()))
                .thenReturn(Optional.empty());
        when(documentRepository.findFirstByChecksumSha256(anyString())).thenReturn(Optional.empty());
        when(authorRepository.findByAuthorName(anyString())).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RestClientException("offline")).when(restTemplate)
                .exchange(any(java.net.URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
    }

    @Test
    void normalizesSupportedDoiForms() {
        assertEquals("10.1000/example", service.normalizeDoi(" DOI:10.1000/Example "));
        assertEquals("10.1000/example", service.normalizeDoi("https://doi.org/10.1000/Example"));
        assertEquals("10.1000/example", service.normalizeDoi("http://dx.doi.org/10.1000/Example"));
    }

    @Test
    void importsDoiOnlyAndKeepsNormalizedDoiWhenProviderIsUnavailable() {
        ArticleDto result = service.importArticle(
                "survey-1",
                new ArticleImportRequest("url", "https://doi.org/10.1000/Example", addedBy(), null, null),
                null
        );

        assertEquals("10.1000/example", result.doi());
        assertEquals("https://doi.org/10.1000/example", result.openUrl());
        assertEquals("doi", result.openType());
    }

    @Test
    void importsTitleAuthorsAndPublicationMetadataFromCrossref() {
        Map<String, Object> message = new HashMap<>();
        message.put("DOI", "10.3390/app9245574");
        message.put("title", List.of("Machine Learning for Quantitative Finance Applications: A Survey"));
        message.put("container-title", List.of("Applied Sciences"));
        message.put("URL", "https://doi.org/10.3390/app9245574");
        message.put("author", List.of(
                Map.of("given", "Francesco", "family", "Rundo"),
                Map.of("given", "Francesca", "family", "Trenta")
        ));
        message.put("published-online", Map.of("date-parts", List.of(List.of(2019, 12, 17))));

        when(restTemplate.exchange(
                argThat((java.net.URI uri) -> uri.toString().contains("api.crossref.org/works/")),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(Map.of("message", message)));

        ArticleDto result = service.importArticle(
                "survey-1",
                new ArticleImportRequest("url", "10.3390/app9245574", addedBy(), null, null),
                null
        );

        assertEquals("Machine Learning for Quantitative Finance Applications: A Survey", result.title());
        assertEquals(List.of("Rundo, Francesco", "Trenta, Francesca"), result.authors());
        assertEquals("Applied Sciences", result.journal());
        assertEquals(2019, result.year());
        assertEquals("10.3390/app9245574", result.doi());
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

        Article saved = captureSavedArticle();
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
        when(articleRepository.findByDoi("10.1000/example")).thenReturn(Optional.of(existing));

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
        assertEquals(1, existing.getSurveyLinks().size());
        assertEquals("Existing", result.title());
        assertEquals(2024, result.year());
    }

    @Test
    void returnsExistingArticleForDuplicatePdfChecksum() throws Exception {
        byte[] pdf = validPdf();
        Article existing = article("article-existing", null);
        Document existingDocument = new Document();
        existingDocument.setArticle(existing);
        existingDocument.setChecksumSha256(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(pdf)
        ));
        when(documentRepository.findFirstByChecksumSha256(existingDocument.getChecksumSha256()))
                .thenReturn(Optional.of(existingDocument));

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
        when(articleRepository.findByExternalId("article-1")).thenReturn(Optional.of(article));

        assertThrows(ResponseStatusException.class, () -> service.findArticlePdf("article-1", null));

        byte[] bytes = "%PDF-test".getBytes();
        Document document = new Document();
        document.setFilePath("legacy.pdf");
        Files.write(storageDir.resolve("legacy.pdf"), bytes);
        document.setMimeType(MediaType.APPLICATION_PDF_VALUE);
        document.setOriginalFileName("paper.pdf");
        when(documentRepository.findFirstByArticleExternalIdAndTypeAndFilePathIsNotNull(
                "article-1", DocumentType.PDF
        )).thenReturn(Optional.of(document));
        AppUser admin = new AppUser(1L, "Admin", "admin@example.com", "hash", "ADMIN");
        when(authSessionRepository.findByToken("token"))
                .thenReturn(Optional.of(new AuthSession(1L, "token", Instant.now(), admin)));

        PdfDownload download = service.findArticlePdf("article-1", "Bearer token");
        assertArrayEquals(bytes, download.content());
        assertEquals("paper.pdf", download.fileName());
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

    private Article captureSavedArticle() {
        var captor = org.mockito.ArgumentCaptor.forClass(Article.class);
        verify(articleRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }
}
