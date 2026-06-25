package mk.ukim.finki.literaturereviewassistant.service.impl;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.literaturereviewassistant.model.*;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleRepository;
import mk.ukim.finki.literaturereviewassistant.repository.AuthorRepository;
import mk.ukim.finki.literaturereviewassistant.repository.AuthSessionRepository;
import mk.ukim.finki.literaturereviewassistant.repository.DocumentRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewerRepository;
import mk.ukim.finki.literaturereviewassistant.repository.SurveyRepository;
import mk.ukim.finki.literaturereviewassistant.service.GeminiService;
import mk.ukim.finki.literaturereviewassistant.service.GemmaService;
import mk.ukim.finki.literaturereviewassistant.service.SurveyService;
import mk.ukim.finki.literaturereviewassistant.service.DataService.ArticleMetadata;
import mk.ukim.finki.literaturereviewassistant.service.DataService.BibEntry;
import mk.ukim.finki.literaturereviewassistant.service.DataService.BibTexParser;
import mk.ukim.finki.literaturereviewassistant.service.DataService.PdfExtractorService;
import mk.ukim.finki.literaturereviewassistant.service.OllamaService;
import mk.ukim.finki.literaturereviewassistant.web.dto.AddedByDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleImportRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleUpdateRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.PdfDownload;
import mk.ukim.finki.literaturereviewassistant.web.dto.ReviewerDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDetailsDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyAskRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.UserResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SurveyServiceImpl implements SurveyService {

    private final SurveyRepository surveyRepository;
    private final ArticleRepository articleRepository;
    private final AuthorRepository authorRepository;
    private final AuthSessionRepository authSessionRepository;
    private final DocumentRepository documentRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewerRepository reviewerRepository;
    private final BibTexParser bibTexParser;
    private final PdfExtractorService pdfExtractorService;
    private final RestTemplate restTemplate;
    private final GeminiService geminiService;
    private final GemmaService gemmaService;

    @Value("${paper.import.max-pdf-size:104857600}")
    private long maxPdfSize;

    @Value("${paper.import.storage-dir:${user.home}/.literature-review-assistant/papers}")
    private String paperStorageDir;

    public SurveyServiceImpl(
            SurveyRepository surveyRepository,
            ArticleRepository articleRepository,
            AuthorRepository authorRepository,
            AuthSessionRepository authSessionRepository,
            DocumentRepository documentRepository,
            ReviewRepository reviewRepository,
            ReviewerRepository reviewerRepository,
            BibTexParser bibTexParser,
            PdfExtractorService pdfExtractorService,
            RestTemplate restTemplate,
            GeminiService geminiService,
            GemmaService gemmaService) {
        this.surveyRepository = surveyRepository;
        this.articleRepository = articleRepository;
        this.authorRepository = authorRepository;
        this.authSessionRepository = authSessionRepository;
        this.documentRepository = documentRepository;
        this.reviewRepository = reviewRepository;
        this.reviewerRepository = reviewerRepository;
        this.bibTexParser = bibTexParser;
        this.pdfExtractorService = pdfExtractorService;
        this.restTemplate = restTemplate;
        this.geminiService = geminiService;
        this.gemmaService = gemmaService;
    }



    @Override
    @Transactional(readOnly = true)
    public SurveyDetailsDto findSurvey(String surveyId) {
        return toSurveyDetailsDto(getSurveyOrThrow(surveyId));
    }

    @Override
    @Transactional
    public SurveyDto saveSurvey(String surveyId, SurveyRequest request, String authorizationHeader) {
        // 1. Check the database to see if this survey actually exists already
        boolean existsInDb = surveyId != null && !surveyId.isBlank() && surveyRepository.findByExternalId(surveyId).isPresent();

        // If it doesn't exist in the DB, treat it as a fresh creation workflow!
        boolean isNew = !existsInDb;

        Survey survey;
        if (existsInDb) {
            // It's an update! Grab the existing one
            survey = surveyRepository.findByExternalId(surveyId).orElseThrow();
        } else {
            // It's a creation! Initialize a fresh entity
            survey = new Survey();
            // Discard the frontend's temporary timestamp ID and assign a clean, secure UUID
            survey.setExternalId("survey-" + java.util.UUID.randomUUID().toString());
            survey.setCreatedDate(LocalDate.now());
        }

        // 2. Map standard request fields safely
        survey.setTitle(request.name());
        survey.setDescription(request.description());
        survey.setStatus(request.status() == null ? "Draft" : request.status());
        if (request.createdDate() != null && !request.createdDate().isBlank()) {
            survey.setCreatedDate(LocalDate.parse(request.createdDate()));
        }
        if (request.researchQuestion() != null) {
            survey.setResearchQuestion(request.researchQuestion());
        } else if (survey.getResearchQuestion() == null) {
            survey.setResearchQuestion("Define the main research question for this survey.");
        }

        // 3. Persist the survey first so the owner association has a stable database identity
        Survey savedSurvey = surveyRepository.save(survey);

        // 4. Bind the owner profile after the survey exists in the database
        ensureOwner(savedSurvey, authorizationHeader);

        // 5. Flush the association update back to the database
        savedSurvey = surveyRepository.save(savedSurvey);

        // 6. Explicitly break out early if it's a creation step to bypass proxy execution loops
        if (isNew) {
            return new SurveyDto(
                    savedSurvey.getExternalId(),
                    savedSurvey.getTitle(),
                    savedSurvey.getDescription(),
                    safeDate(savedSurvey.getCreatedDate()),
                    savedSurvey.getStatus(),
                    0 // Brand new, no articles exist yet!
            );
        }

        return toSurveyDto(savedSurvey);
    }

    @Override
    @Transactional
    public void deleteSurvey(String surveyId) {
        // 1. Find the survey or throw an error if it doesn't exist
        Survey survey = surveyRepository.findByExternalId(surveyId)
                .orElseThrow(() -> new EntityNotFoundException("Survey not found with external id: " + surveyId));

        // 2. Clear out the Many-to-Many associations with Reviewers
        // This removes rows from the hidden 'reviewer_survey' table safely
        for (Reviewer reviewer : new ArrayList<>(survey.getReviewers())) {
            reviewer.getSurveys().remove(survey);
        }
        survey.getReviewers().clear();

        // 3. Clear out the Article links
        // Since 'articleLinks' has cascade = CascadeType.ALL and orphanRemoval = true,
        // clearing this collection automatically deletes the rows from 'article_survey'
        survey.getArticleLinks().clear();

        // 4. Safely delete the survey entity from the database
        surveyRepository.delete(survey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleDto> findArticles(String surveyId) {
        Survey survey = getSurveyOrThrow(surveyId);
        return articleRepository.findBySurveyLinks_Survey(survey).stream().map(this::toArticleDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleDto findArticle(String articleId) {
        return toArticleDto(getArticleOrThrow(articleId));
    }

    @Override
    @Transactional(readOnly = true)
    public PdfDownload findArticlePdf(String articleId, String authorizationHeader) {
        Article article = getArticleOrThrow(articleId);
        AppUser currentUser = currentUserRequired(authorizationHeader);
        if (!canAccessArticle(article, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this article");
        }

        Document document = documentRepository
                .findFirstByArticleExternalIdAndTypeAndFilePathIsNotNull(article.getExternalId(), DocumentType.PDF)
                .orElseGet(() -> documentRepository
                        .findFirstByArticleExternalIdAndTypeAndPdfContentIsNotNull(article.getExternalId(), DocumentType.PDF)
                        .orElseThrow(() -> new EntityNotFoundException("PDF not found for article: " + articleId)));

        return new PdfDownload(
                readPdfBytes(document, article),
                defaultString(document.getMimeType(), MediaType.APPLICATION_PDF_VALUE),
                safeDownloadFileName(document.getOriginalFileName(), article.getExternalId())
        );
    }

    @Override
    @Transactional
    public ArticleDto importArticle(String surveyId, ArticleImportRequest request, String authorizationHeader) {
        Survey survey = getSurveyOrThrow(surveyId);
        AddedByDto addedBy = resolveAddedBy(survey, request.addedBy(), authorizationHeader);

        if ("bibtex".equalsIgnoreCase(request.type())) {
            List<BibEntry> entries = bibTexParser.parse(request.data() == null ? "" : request.data());
            List<Article> imported = new ArrayList<>();
            for (BibEntry entry : entries) {
                ArticleMetadata metadata = metadataFromBibEntry(entry);
                if (!hasText(metadata.getDoi()) && !hasText(metadata.getUrl())) {
                    metadata = mergeMissingMetadata(metadata, fetchCrossrefMetadataByTitle(metadata.getTitle(), metadata.getYear()));
                }
                imported.add(saveImportedArticle(survey, addedBy, metadata, null));
            }
            if (imported.isEmpty()) {
                throw new EntityNotFoundException("No BibTeX entries could be parsed");
            }
            return toArticleDto(imported.get(0));
        }

        if ("pdf".equalsIgnoreCase(request.type())) {
            byte[] pdfBytes = decodePdfBytes(request.data());
            validatePdf(pdfBytes, request.mimeType());

            String fileName = request.fileName() == null || request.fileName().isBlank()
                    ? "uploaded.pdf"
                    : request.fileName();
            ArticleMetadata metadata = pdfExtractorService.extractFromBytes(pdfBytes, fileName);
            metadata.setPdfBytes(pdfBytes);
            return toArticleDto(saveImportedArticle(survey, addedBy, metadata, fileName));
        }

        ArticleMetadata metadata = resolveMetadataFromInput(request.data());
        return toArticleDto(saveImportedArticle(survey, addedBy, metadata, null));
    }

    @Override
    @Transactional
    public ArticleDto importPdfArticle(String surveyId, MultipartFile file, AddedByDto addedBy) {
        Survey survey = getSurveyOrThrow(surveyId);
        AddedByDto author = addedBy == null ? new AddedByDto("owner", "Survey Owner", "Owner") : addedBy;
        byte[] bytes = readBytes(file);
        validatePdf(bytes, file.getContentType());
        ArticleMetadata metadata = pdfExtractorService.extractFromBytes(bytes, file.getOriginalFilename());
        return toArticleDto(saveImportedArticle(survey, author, metadata, file.getOriginalFilename()));
    }

    @Override
    @Transactional
    public ArticleDto updateArticle(String surveyId, String articleId, ArticleUpdateRequest request, String authorizationHeader) {
        Survey survey = getSurveyOrThrow(surveyId);
        Article article = articleRepository.findByExternalId(articleId)
                .filter(item -> belongsToSurvey(item, survey))
                .orElseThrow(() -> new EntityNotFoundException("Article not found: " + articleId));

        AppUser currentUser = currentUserRequired(authorizationHeader);
        if (!canEditArticle(survey, article, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit articles you added, unless you are the survey owner");
        }

        if (request.title() != null && !request.title().isBlank()) {
            article.setTitle(request.title().trim());
        }
        if (request.journal() != null && !request.journal().isBlank()) {
            article.setJournal(request.journal().trim());
        }
        if (request.doi() != null) {
            article.setDoi(request.doi().isBlank() ? null : normalizeDoi(request.doi()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            article.setStatus(request.status().trim());
        }
        if (request.abstractText() != null) {
            article.setArticleAbstract(request.abstractText().trim());
        }
        if (request.inclusionSummary() != null) {
            article.setInclusionSummary(request.inclusionSummary().trim());
        }
        if (request.year() != null) {
            article.setPublicationYear(request.year());
        }
        if (request.authors() != null) {
            article.setAuthors(resolveOrCreateAuthors(request.authors()));
        }

        return toArticleDto(articleRepository.save(article));
    }

    @Override
    @Transactional
    public void deleteArticle(String surveyId, String articleId, String authorizationHeader) {
        Survey survey = getSurveyOrThrow(surveyId);
        Article article = articleRepository.findByExternalId(articleId)
                .filter(item -> belongsToSurvey(item, survey))
                .orElseThrow(() -> new EntityNotFoundException("Article not found: " + articleId));

        AppUser currentUser = currentUserRequired(authorizationHeader);
        if (!canEditArticle(survey, article, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete articles you added, unless you are the survey owner");
        }

        deleteStoredFiles(article);
        articleRepository.delete(article);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewerDto> findReviewers(String surveyId) {
        return reviewerRepository.findBySurveysContaining(getSurveyOrThrow(surveyId)).stream().map(this::toReviewerDto).toList();
    }

    @Override
    @Transactional
    public ReviewerDto addReviewer(String surveyId, ReviewerDto reviewer, String authorizationHeader) {
        Survey survey = getSurveyOrThrow(surveyId);
        ensureOwnerAccess(survey, authorizationHeader);
        Reviewer saved = reviewerRepository.findBySurveysContainingAndExternalId(survey, reviewer.id()).orElseGet(Reviewer::new);
        saved.setExternalId(reviewer.id());
        saved.setName(reviewer.name());
        saved.setEmail(reviewer.email());
        saved.setRole(reviewer.role() == null ? "Reviewer" : reviewer.role());
        saved.setAddedDate(reviewer.addedDate() == null ? Instant.now() : Instant.parse(reviewer.addedDate()));

        if (!saved.getSurveys().contains(survey)) {
            saved.getSurveys().add(survey);
        }

        return toReviewerDto(reviewerRepository.save(saved));
    }

    @Override
    @Transactional
    public void removeReviewer(String surveyId, String reviewerId, String authorizationHeader) {
        Survey survey = getSurveyOrThrow(surveyId);
        ensureOwnerAccess(survey, authorizationHeader);

        reviewerRepository.findBySurveysContainingAndExternalId(survey, reviewerId)
                .filter(reviewer -> !"Owner".equals(reviewer.getRole()))
                .ifPresent(reviewer -> {
                    if (survey.getReviewers() != null) {
                        survey.getReviewers().remove(reviewer);
                    }
                    if (reviewer.getSurveys() != null) {
                        reviewer.getSurveys().remove(survey);
                    }
                    surveyRepository.save(survey);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewerDto> searchReviewersByEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }

        // Fetch matching reviewer entities from the database
        return reviewerRepository.findByEmailContainingIgnoreCase(email.trim())
                .stream()
                .map(this::toReviewerDto) // Map entity to DTO
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String askSurvey(String surveyId, SurveyAskRequest request) {
        Survey survey = getSurveyOrThrow(surveyId);
        String question = request == null || request.question() == null ? "" : request.question().trim();
        if (question.isBlank()) {
            throw new IllegalArgumentException("Question is required");
        }

        List<Article> articles = articleRepository.findBySurveyExternalId(surveyId);
        String context = buildSurveyContext(survey, articles);
        String prompt = """
                You are helping review a literature survey.
                Use the survey context below when it is relevant to the user's question.
                If the question is not about the survey context, answer it normally using general knowledge.
                If the context is relevant but incomplete, say what the survey does and does not support.
                Do not mention that you are an AI model.

                Survey title: %s
                Survey research question: %s

                Survey context:
                %s

                User question:
                %s
                """.formatted(
                defaultString(survey.getTitle(), "Untitled Survey"),
                defaultString(survey.getResearchQuestion(), ""),
                context,
                question
        );

        System.out.println("Prompt: ");
        System.out.println(prompt);

        return gemmaService.callGemma(prompt);
    }

    private Survey getSurveyOrThrow(String surveyId) {
        return surveyRepository.findByExternalId(surveyId)
                .orElseThrow(() -> new EntityNotFoundException("Survey not found: " + surveyId));
    }

    private Article getArticleOrThrow(String articleId) {
        return articleRepository.findByExternalId(articleId)
                .or(() -> parseLong(articleId).flatMap(articleRepository::findById))
                .orElseThrow(() -> new EntityNotFoundException("Article not found: " + articleId));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read uploaded PDF", e);
        }
    }

    private java.util.Optional<Long> parseLong(String value) {
        try {
            return java.util.Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }

    private Author resolveAuthor(String name) {
        return authorRepository.findByAuthorName(name).orElseGet(() -> {
            Author author = new Author();
            author.setAuthorName(name);
            return authorRepository.save(author);
        });
    }

    private List<Author> resolveOrCreateAuthors(List<String> authorNames) {
        if (authorNames == null) {
            return List.of();
        }

        List<Author> authors = new ArrayList<>();
        for (String name : authorNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            authors.add(resolveAuthor(name.trim()));
        }
        return authors;
    }

    private ArticleMetadata metadataFromBibEntry(BibEntry entry) {
        ArticleMetadata metadata = new ArticleMetadata();
        metadata.setTitle(entry.getTitle());
        metadata.setDoi(normalizeDoi(entry.getDoi()));
        metadata.setUrl(normalizeExternalUrl(entry.getUrl()));
        metadata.setJournal(entry.getJournal());
        if (entry.getYear() != null && !entry.getYear().isBlank()) {
            try {
                metadata.setYear(Integer.parseInt(entry.getYear().trim()));
            } catch (NumberFormatException ignored) {
                // ignore invalid year
            }
        }
        metadata.setAbstractText(entry.getAbstractText());
        metadata.setAuthorNames(entry.getAuthorNames());
        return metadata;
    }

    private ArticleMetadata resolveMetadataFromInput(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Article input is empty");
        }

        if (trimmed.startsWith("@")) {
            List<BibEntry> entries = bibTexParser.parse(trimmed);
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("No BibTeX entries could be parsed");
            }
            return metadataFromBibEntry(entries.get(0));
        }

        if (looksLikeDoi(trimmed)) {
            return resolveMetadataFromDoi(trimmed);
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            if (trimmed.toLowerCase().contains("doi.org/")) {
                return resolveMetadataFromDoi(extractDoi(trimmed));
            }

            if (trimmed.toLowerCase().endsWith(".pdf") || trimmed.toLowerCase().contains("pdf")) {
                return pdfExtractorService.extractFromUrl(trimmed);
            }

            ArticleMetadata metadata = pdfExtractorService.extractFromUrl(trimmed);
            if (isMetadataUsable(metadata)) {
                return metadata;
            }

            metadata = new ArticleMetadata();
            metadata.setTitle(extractTitleFromUrl(trimmed));
            metadata.setUrl(trimmed);
            return metadata;
        }

        return resolveMetadataFromDoi(trimmed);
    }

    private ArticleMetadata resolveMetadataFromDoi(String rawInput) {
        String doi = normalizeDoi(rawInput);
        if (doi == null || doi.isBlank()) {
            ArticleMetadata fallback = new ArticleMetadata();
            fallback.setTitle(extractTitleFromUrl(rawInput));
            fallback.setDoi(rawInput);
            return fallback;
        }

        ArticleMetadata resolved = new ArticleMetadata();
        resolved.setDoi(doi);

        // Crossref generally provides the most complete structured title, author,
        // publication and date metadata. Other providers only fill missing fields.
        resolved = mergeMissingMetadata(resolved, fetchCrossrefMetadata(doi));
        resolved = mergeMissingMetadata(resolved, fetchDoiOrgMetadata(doi));
        resolved = mergeMissingMetadata(resolved, fetchDoiBibtexMetadata(doi));
        resolved = mergeMissingMetadata(resolved, fetchLandingPageMetadata(doi));

        if (!hasDescriptiveMetadata(resolved)) {
            resolved.setTitle(extractTitleFromDoi(doi));
        }
        return resolved;
    }

    private ArticleMetadata fetchDoiBibtexMetadata(String doi) {
        URI apiUri = doiUri("https://doi.org", doi);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.valueOf("application/x-bibtex")));
            headers.set(HttpHeaders.USER_AGENT, "literature-review-assistant/1.0 (mailto:unknown@example.com)");

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            String body = response.getBody();
            if (body != null && !body.isBlank()) {
                List<BibEntry> entries = bibTexParser.parse(body);
                if (!entries.isEmpty()) {
                    return metadataFromBibEntry(entries.get(0));
                }
            }
        } catch (Exception e) {
            System.err.println("DOI BibTeX lookup failed for DOI " + doi + ": " + e.getMessage());
        }

        return new ArticleMetadata();
    }

    private ArticleMetadata fetchCrossrefMetadata(String doi) {
        URI apiUri = doiUri("https://api.crossref.org/works", doi);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set(HttpHeaders.USER_AGENT, "literature-review-assistant/1.0 (mailto:unknown@example.com)");

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (response.getBody() != null) {
                Object message = response.getBody().get("message");
                if (message instanceof Map<?, ?> messageMap) {
                    return metadataFromCrossref((Map<String, Object>) messageMap, doi);
                }
            }
        } catch (Exception e) {
            System.err.println("Crossref lookup failed for DOI " + doi + ": " + e.getMessage());
        }

        return new ArticleMetadata();
    }

    @SuppressWarnings("unchecked")
    private ArticleMetadata fetchCrossrefMetadataByTitle(String title, Integer year) {
        if (!hasText(title)) {
            return new ArticleMetadata();
        }

        URI apiUri = UriComponentsBuilder.fromUriString("https://api.crossref.org/works")
                .queryParam("query.title", title)
                .queryParam("rows", 5)
                .build()
                .encode()
                .toUri();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set(HttpHeaders.USER_AGENT, "literature-review-assistant/1.0 (mailto:unknown@example.com)");

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Object message = response.getBody() == null ? null : response.getBody().get("message");
            if (!(message instanceof Map<?, ?> messageMap) || !(messageMap.get("items") instanceof List<?> items)) {
                return new ArticleMetadata();
            }

            String normalizedTitle = normalizeTitle(title);
            ArticleMetadata best = null;
            double bestScore = 0;
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> rawItem)) {
                    continue;
                }
                ArticleMetadata candidate = metadataFromCrossref((Map<String, Object>) rawItem, null);
                double score = titleSimilarity(normalizedTitle, normalizeTitle(candidate.getTitle()));
                if (year != null && candidate.getYear() != null && year.equals(candidate.getYear())) {
                    score += 0.2;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }

            return bestScore >= 0.65 && best != null ? best : new ArticleMetadata();
        } catch (Exception e) {
            System.err.println("Crossref title lookup failed for " + title + ": " + e.getMessage());
            return new ArticleMetadata();
        }
    }

    private ArticleMetadata fetchDoiOrgMetadata(String doi) {
        URI apiUri = doiUri("https://doi.org", doi);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.valueOf("application/vnd.citationstyles.csl+json")));
            headers.set(HttpHeaders.USER_AGENT, "literature-review-assistant/1.0 (mailto:unknown@example.com)");

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (response.getBody() != null) {
                return metadataFromDoiOrg(response.getBody(), doi);
            }
        } catch (Exception e) {
            System.err.println("DOI.org lookup failed for DOI " + doi + ": " + e.getMessage());
        }

        return new ArticleMetadata();
    }

    private ArticleMetadata metadataFromCrossref(Map<String, Object> message, String doi) {
        ArticleMetadata metadata = new ArticleMetadata();
        metadata.setDoi(normalizeDoi(stringValue(message.getOrDefault("DOI", doi))));
        metadata.setTitle(firstTextValue(message.get("title")));
        metadata.setAbstractText(cleanCrossrefAbstract(stringValue(message.get("abstract"))));
        metadata.setUrl(normalizeExternalUrl(stringValue(message.get("URL"))));
        metadata.setJournal(firstTextValue(message.get("container-title")));
        metadata.setAuthorNames(parseCrossrefAuthors(message.get("author")));
        Integer year = extractCrossrefYear(message);
        if (year != null) {
            metadata.setYear(year);
        }
        if ((metadata.getTitle() == null || metadata.getTitle().isBlank())) {
            metadata.setTitle(extractTitleFromDoi(doi));
        }
        return metadata;
    }

    private ArticleMetadata metadataFromDoiOrg(Map<String, Object> message, String doi) {
        ArticleMetadata metadata = new ArticleMetadata();
        metadata.setDoi(normalizeDoi(defaultString(stringValue(message.getOrDefault("DOI", doi)), doi)));
        metadata.setTitle(firstTextValue(message.get("title")));
        metadata.setAbstractText(cleanCrossrefAbstract(stringValue(message.get("abstract"))));
        metadata.setUrl(normalizeExternalUrl(stringValue(message.get("URL"))));
        metadata.setJournal(firstTextValue(message.get("container-title")));
        metadata.setAuthorNames(parseDoiOrgAuthors(message.get("author")));
        Integer year = extractDoiOrgYear(message);
        if (year != null) {
            metadata.setYear(year);
        }
        if (!hasText(metadata.getTitle())) {
            metadata.setTitle(extractTitleFromDoi(doi));
        }
        return metadata;
    }

    private ArticleMetadata fetchLandingPageMetadata(String doi) {
        URI apiUri = doiUri("https://doi.org", doi);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML));
            headers.set(HttpHeaders.USER_AGENT, "literature-review-assistant/1.0 (mailto:unknown@example.com)");

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getBody() != null) {
                return metadataFromHtml(response.getBody(), doi, apiUri.toString());
            }
        } catch (Exception e) {
            System.err.println("DOI landing page lookup failed for DOI " + doi + ": " + e.getMessage());
        }

        return new ArticleMetadata();
    }

    private URI doiUri(String baseUrl, String doi) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/")
                .path(doi)
                .build()
                .encode()
                .toUri();
    }

    private ArticleMetadata metadataFromHtml(String html, String doi, String sourceUrl) {
        ArticleMetadata metadata = new ArticleMetadata();
        metadata.setDoi(doi);
        metadata.setUrl(sourceUrl);
        metadata.setTitle(firstMatchingMeta(html, "citation_title", "og:title", "dc.title"));
        metadata.setJournal(firstMatchingMeta(html, "citation_journal_title", "prism.publicationName", "dc.source"));
        metadata.setAbstractText(firstMatchingMeta(html, "citation_abstract", "description", "og:description"));
        metadata.setAuthorNames(parseHtmlAuthors(html));
        String year = firstMatchingMeta(html, "citation_publication_date", "citation_year");
        if (year != null) {
            Integer parsedYear = parseYearFromString(year);
            if (parsedYear != null) {
                metadata.setYear(parsedYear);
            }
        }
        if (!hasText(metadata.getTitle())) {
            metadata.setTitle(extractTitleFromDoi(doi));
        }
        return metadata;
    }

    // Add this method inside your SurveyServiceImpl class

    @Override
    @Transactional
    public SurveyDetailsDto createNewSurveyWithArticle(String name, String initialArticleId, String authorizationToken) {
        // 1. Fetch the source article template
        Article sourceArticle = articleRepository.findByExternalId(initialArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source article not found"));

        // 2. Instantiate the Survey shell structure
        Survey survey = new Survey();
        survey.setExternalId(java.util.UUID.randomUUID().toString());
        survey.setTitle(name);
        survey.setDescription("Survey created dynamically from referenced AI discovery asset.");
        survey.setResearchQuestion("Define your research target query parameter here.");
        survey.setCreatedDate(java.time.LocalDate.now());
        survey.setStatus("In Progress");
        survey.setArticleLinks(new ArrayList<>());
        survey.setReviewers(new ArrayList<>());

        // Attach owner logic here (Spring Security block from previous step)
        // ...

        // 3. Create the link entity and bind BOTH sides cleanly in memory
        ArticleSurvey associationLink = new ArticleSurvey();
        associationLink.setSurvey(survey);         // Link back to our survey shell
        associationLink.setArticle(sourceArticle); // Link forward to our article asset

        // Set status based on your domain implementation type (Enum or String)
        try {
            associationLink.setStatus(ArticleStatus.PENDING);
        } catch (Exception e) {
            // Fallback if your entity uses a standard String field instead of an Enum
            // associationLink.setStatus("PENDING");
        }

        // 4. Add the link directly to the survey's own internal managed collection
        survey.getArticleLinks().add(associationLink);

        // 5. Save the survey. Because cascading is enabled on your OneToMany collection,
        // Hibernate will automatically discover the associationLink and save it for you.
        Survey savedSurvey = surveyRepository.save(survey);

        // 6. Project structural entity context directly back into your UI data record layout
        return toSurveyDetailsDto(savedSurvey);
    }

    private Article saveImportedArticle(Survey survey, AddedByDto addedBy, ArticleMetadata metadata, String fallbackFileName) {
        metadata.setDoi(normalizeDoi(metadata.getDoi()));
        metadata.setUrl(normalizeExternalUrl(metadata.getUrl()));

        byte[] pdfBytes = metadata.getPdfBytes();
        if (pdfBytes != null && pdfBytes.length > 0) {
            String checksum = sha256(pdfBytes);
            Optional<Document> existingDocument = documentRepository.findFirstByChecksumSha256(checksum);
            if (existingDocument.isPresent()) {
                return linkArticleToSurvey(existingDocument.get().getArticle(), survey);
            }
        }

        if (hasText(metadata.getDoi())) {
            Optional<Article> existing = articleRepository.findByDoi(metadata.getDoi());
            if (existing.isPresent()) {
                Article existingArticle = existing.get();
                enrichExistingArticle(existingArticle, metadata);
                return linkArticleToSurvey(existingArticle, survey);
            }
        }

        if (hasText(metadata.getTitle()) && metadata.getYear() != null) {
            Optional<Article> existing = articleRepository
                    .findFirstByTitleIgnoreCaseAndPublicationYear(metadata.getTitle().trim(), metadata.getYear());
            if (existing.isPresent()) {
                return linkArticleToSurvey(existing.get(), survey);
            }
        }

        Article article = new Article();
        article.setExternalId("article-" + UUID.randomUUID());
        article.setSurveyExternalId(survey.getExternalId());
        article.setTitle(defaultString(metadata.getTitle(), "Untitled Article"));
        article.setDoi(defaultString(metadata.getDoi(), null));
        article.setUrl(defaultString(metadata.getUrl(), null));
        article.setJournal(defaultString(metadata.getJournal(), "Imported Journal"));
        article.setPublicationYear(metadata.getYear() != null ? metadata.getYear() : LocalDate.now().getYear());
        article.setStatus("PENDING");
        article.setArticleAbstract(defaultString(metadata.getAbstractText(), "This article was imported and needs to be reviewed."));
        article.setAddedById(addedBy.id());
        article.setAddedByName(addedBy.name());
        article.setAddedByRole(addedBy.role());
        article.setAuthors(resolveOrCreateAuthors(metadata.getAuthorNames()));

        ArticleSurvey link = new ArticleSurvey();
        link.setArticle(article);
        link.setSurvey(survey);
        link.setStatus(mk.ukim.finki.literaturereviewassistant.model.ArticleStatus.PENDING); // Default join enum status

        article.getSurveyLinks().add(link);
        survey.getArticleLinks().add(link);

        Article saved = articleRepository.save(article);
        attachPdfDocumentIfPresent(saved, metadata, fallbackFileName);
        return articleRepository.save(saved);
    }

    private boolean isMetadataUsable(ArticleMetadata metadata) {
        return metadata != null &&
                (hasText(metadata.getTitle()) || hasText(metadata.getDoi()) || hasText(metadata.getAbstractText()));
    }

    private boolean hasDescriptiveMetadata(ArticleMetadata metadata) {
        return metadata != null && (
                hasText(metadata.getTitle())
                        || hasText(metadata.getJournal())
                        || (metadata.getAuthorNames() != null && !metadata.getAuthorNames().isEmpty())
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean looksLikeDoi(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return input.startsWith("10.") || lower.contains("doi.org/") || lower.startsWith("doi:");
    }

    private String extractDoi(String input) {
        return normalizeDoi(input);
    }

    String normalizeDoi(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("doi:")) {
            trimmed = trimmed.substring(4).trim();
        }

        lower = trimmed.toLowerCase(Locale.ROOT);
        for (String prefix : List.of(
                "https://doi.org/",
                "http://doi.org/",
                "https://dx.doi.org/",
                "http://dx.doi.org/"
        )) {
            if (lower.startsWith(prefix)) {
                trimmed = trimmed.substring(prefix.length());
                break;
            }
        }

        trimmed = trimmed.replaceAll("[?#].*$", "").trim();
        try {
            trimmed = URLDecoder.decode(trimmed, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Keep the undecoded DOI when malformed percent escapes are supplied.
        }
        return trimmed.replaceAll("^[\\s<]+|[\\s>.,;]+$", "").toLowerCase(Locale.ROOT);
    }

    private String extractTitleFromDoi(String doi) {
        String slug = doi.replace("/", " ").replace(".", " ").replace("-", " ").trim();
        if (slug.isBlank()) {
            return "Imported DOI Article";
        }
        return "Imported article for " + slug;
    }

    private String extractTitleFromUrl(String url) {
        String cleaned = url.replaceAll("\\?.*$", "");
        int slash = cleaned.lastIndexOf('/');
        String last = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
        last = last.replaceAll("\\.[A-Za-z0-9]+$", "").replace('-', ' ').replace('_', ' ').trim();
        return last.isBlank() ? "Imported Article" : last;
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String normalizeExternalUrl(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            return uri.toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private ArticleMetadata mergeMissingMetadata(ArticleMetadata primary, ArticleMetadata fallback) {
        if (fallback == null) {
            return primary;
        }
        if (!hasText(primary.getTitle())) primary.setTitle(fallback.getTitle());
        if (!hasText(primary.getDoi())) primary.setDoi(normalizeDoi(fallback.getDoi()));
        if (!hasText(primary.getUrl())) primary.setUrl(normalizeExternalUrl(fallback.getUrl()));
        if (!hasText(primary.getJournal())) primary.setJournal(fallback.getJournal());
        if (!hasText(primary.getAbstractText())) primary.setAbstractText(fallback.getAbstractText());
        if (primary.getYear() == null) primary.setYear(fallback.getYear());
        if ((primary.getAuthorNames() == null || primary.getAuthorNames().isEmpty())
                && fallback.getAuthorNames() != null) {
            primary.setAuthorNames(fallback.getAuthorNames());
        }
        return primary;
    }

    private String normalizeTitle(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private double titleSimilarity(String left, String right) {
        if (left.isBlank() || right.isBlank()) {
            return 0;
        }
        if (left.equals(right)) {
            return 1;
        }
        var leftWords = new java.util.HashSet<>(List.of(left.split("\\s+")));
        var rightWords = new java.util.HashSet<>(List.of(right.split("\\s+")));
        var intersection = new java.util.HashSet<>(leftWords);
        intersection.retainAll(rightWords);
        var union = new java.util.HashSet<>(leftWords);
        union.addAll(rightWords);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private Article linkArticleToSurvey(Article article, Survey survey) {
        boolean alreadyLinked = article.getSurveyLinks().stream()
                .anyMatch(link -> link.getSurvey().getSurveyId().equals(survey.getSurveyId()));
        if (!alreadyLinked) {
            ArticleSurvey link = new ArticleSurvey();
            link.setArticle(article);
            link.setSurvey(survey);
            link.setStatus(mk.ukim.finki.literaturereviewassistant.model.ArticleStatus.PENDING);
            article.getSurveyLinks().add(link);
            survey.getArticleLinks().add(link);
        }
        return articleRepository.save(article);
    }

    private boolean belongsToSurvey(Article article, Survey survey) {
        if (article == null || survey == null) {
            return false;
        }
        if (survey.getExternalId() != null && survey.getExternalId().equals(article.getSurveyExternalId())) {
            return true;
        }
        return article.getSurveyLinks().stream()
                .anyMatch(link -> link.getSurvey() != null
                        && survey.getExternalId() != null
                        && survey.getExternalId().equals(link.getSurvey().getExternalId()));
    }

    private void enrichExistingArticle(Article article, ArticleMetadata metadata) {
        if ((!hasText(article.getTitle()) || article.getTitle().startsWith("Imported article for "))
                && hasText(metadata.getTitle())) {
            article.setTitle(metadata.getTitle().trim());
        }
        if ((!hasText(article.getJournal()) || "Imported Journal".equals(article.getJournal()))
                && hasText(metadata.getJournal())) {
            article.setJournal(metadata.getJournal().trim());
        }
        if ((!hasText(article.getArticleAbstract())
                || "This article was imported and needs to be reviewed.".equals(article.getArticleAbstract()))
                && hasText(metadata.getAbstractText())) {
            article.setArticleAbstract(metadata.getAbstractText().trim());
        }
        if (!hasText(article.getUrl()) && hasText(metadata.getUrl())) {
            article.setUrl(normalizeExternalUrl(metadata.getUrl()));
        }
        if (metadata.getYear() != null
                && (article.getPublicationYear() == null || article.getPublicationYear().equals(LocalDate.now().getYear()))) {
            article.setPublicationYear(metadata.getYear());
        }
        if ((article.getAuthors() == null || article.getAuthors().isEmpty())
                && metadata.getAuthorNames() != null
                && !metadata.getAuthorNames().isEmpty()) {
            article.setAuthors(resolveOrCreateAuthors(metadata.getAuthorNames()));
        }
    }

    private String firstMatchingMeta(String html, String... names) {
        if (html == null || html.isBlank()) {
            return null;
        }

        for (String name : names) {
            String content = matchMetaTag(html, name);
            if (hasText(content)) {
                return content;
            }
        }

        return null;
    }

    private String matchMetaTag(String html, String name) {
        String[] patterns = {
                "<meta[^>]+name=[\"']" + name + "[\"'][^>]+content=[\"']([^\"']+)[\"'][^>]*>",
                "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+name=[\"']" + name + "[\"'][^>]*>",
                "<meta[^>]+property=[\"']" + name + "[\"'][^>]+content=[\"']([^\"']+)[\"'][^>]*>",
                "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']" + name + "[\"'][^>]*>"
        };

        for (String pattern : patterns) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
            if (matcher.find()) {
                return htmlDecode(matcher.group(1));
            }
        }

        return null;
    }

    private String htmlDecode(String value) {
        return value
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private List<String> parseHtmlAuthors(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }

        List<String> authors = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("<meta[^>]+name=[\"']citation_author[\"'][^>]+content=[\"']([^\"']+)[\"'][^>]*>",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html);
        while (matcher.find()) {
            String author = htmlDecode(matcher.group(1)).trim();
            if (!author.isBlank()) {
                authors.add(author);
            }
        }
        return authors;
    }

    private Integer parseYearFromString(String value) {
        if (value == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(19|20)\\d{2}").matcher(value);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String cleanCrossrefAbstract(String abstractText) {
        if (!hasText(abstractText)) {
            return null;
        }
        return abstractText.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private String firstTextValue(Object value) {
        if (value instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first == null ? null : first.toString();
        }
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseDoiOrgAuthors(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<String> authors = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> authorMap) {
                String family = authorMap.get("family") == null ? "" : authorMap.get("family").toString().trim();
                String given = authorMap.get("given") == null ? "" : authorMap.get("given").toString().trim();
                String combined = (family + ", " + given).replaceAll(",\\s*$", "").trim();
                if (!combined.isBlank()) {
                    authors.add(combined);
                }
            } else if (item != null) {
                String text = item.toString().trim();
                if (!text.isBlank()) {
                    authors.add(text);
                }
            }
        }
        return authors;
    }

    @SuppressWarnings("unchecked")
    private Integer extractDoiOrgYear(Map<String, Object> message) {
        Object issued = message.get("issued");
        if (issued instanceof Map<?, ?> map) {
            Object dateParts = map.get("date-parts");
            if (dateParts instanceof List<?> outer && !outer.isEmpty()) {
                Object inner = outer.get(0);
                if (inner instanceof List<?> years && !years.isEmpty()) {
                    Object first = years.get(0);
                    if (first != null) {
                        try {
                            return Integer.parseInt(first.toString());
                        } catch (NumberFormatException ignored) {
                            // continue
                        }
                    }
                }
            }
        }

        return extractCrossrefYear(message);
    }

    @SuppressWarnings("unchecked")
    private List<String> parseCrossrefAuthors(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<String> authors = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> authorMap) {
                String given = authorMap.get("given") == null ? "" : authorMap.get("given").toString().trim();
                String family = authorMap.get("family") == null ? "" : authorMap.get("family").toString().trim();
                String combined = (family + ", " + given).replaceAll(",\\s*$", "").trim();
                if (!combined.isBlank()) {
                    authors.add(combined);
                }
            }
        }
        return authors;
    }

    @SuppressWarnings("unchecked")
    private Integer extractCrossrefYear(Map<String, Object> message) {
        Object[] candidates = {
                message.get("published-print"),
                message.get("published-online"),
                message.get("created")
        };

        for (Object candidate : candidates) {
            if (candidate instanceof Map<?, ?> map) {
                Object dateParts = map.get("date-parts");
                if (dateParts instanceof List<?> outer && !outer.isEmpty()) {
                    Object inner = outer.get(0);
                    if (inner instanceof List<?> years && !years.isEmpty()) {
                        Object first = years.get(0);
                        if (first != null) {
                            try {
                                return Integer.parseInt(first.toString());
                            } catch (NumberFormatException ignored) {
                                // continue
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyDto> findAllSurveys(String authorizationHeader) {
        Optional<AppUser> currentUser = currentUser(authorizationHeader);
        if (currentUser.isEmpty()) {
            return List.of();
        }
        if (currentUser.map(u -> u.getRole().equals("ADMIN")).orElse(false)) {
            return surveyRepository.findAll().stream().map(this::toSurveyDto).toList();
        }
        return surveyRepository.findAll().stream()
                .filter(survey -> survey.getReviewers().stream()
                        .anyMatch(contributor -> contributor.getEmail() != null 
                                && currentUser.get().getEmail() != null 
                                && contributor.getEmail().equalsIgnoreCase(currentUser.get().getEmail())
                                && "Owner".equals(contributor.getRole())))
                .map(this::toSurveyDto)
                .toList();
    }
    private void ensureOwner(Survey survey, String authorizationHeader) {
        Optional<AppUser> currentUserOpt = currentUser(authorizationHeader);

        // 1. Initialize collections safely
        if (survey.getReviewers() == null) {
            survey.setReviewers(new java.util.ArrayList<>());
        }

        if (currentUserOpt.isPresent()) {
            AppUser user = currentUserOpt.get();

//             // 2. Look up if this user already has an Owner Reviewer record (by AppUser + role).
//             //    Since a user can also be a Reviewer on other surveys, we need to find specifically
//             //    their "Owner" record (or create one if it doesn't exist yet).
//             Optional<Reviewer> existingOwnerOpt = reviewerRepository.findByAppUserAndRole(user, "Owner");

//             if (existingOwnerOpt.isPresent()) {
//                 Reviewer existing = existingOwnerOpt.get();
//                 // If they are already associated with this survey, do nothing
//                 if (!existing.getSurveys().contains(survey)) {
//                     existing.getSurveys().add(survey);
//                     reviewerRepository.save(existing);
//                 }
//             } else {
//                 // 3. No Owner Reviewer record for this AppUser yet — create one
//                 List<Survey> associatedSurveys = new java.util.ArrayList<>();
//                 associatedSurveys.add(survey);

//                 Reviewer newOwner = new Reviewer(
//                         null,
//                         java.util.UUID.randomUUID().toString(),
//                         user.getName(),
//                         user.getEmail(),
//                         "Owner",
//                         Instant.now(),
//                         user,
//                         associatedSurveys,
//                         new java.util.ArrayList<>()
//                 );
//                 reviewerRepository.save(newOwner);
//             }
// =======
            // 2. Safely find or create the reviewer
            Optional<Reviewer> existingReviewerOpt = reviewerRepository.findFirstByEmail(user.getEmail());
            Reviewer targetOwner;

            if (existingReviewerOpt.isPresent()) {
                targetOwner = existingReviewerOpt.get();
            } else {
                targetOwner = new Reviewer();
                targetOwner.setExternalId(java.util.UUID.randomUUID().toString());
                targetOwner.setName(user.getName());
                targetOwner.setEmail(user.getEmail());
                targetOwner.setAddedDate(Instant.now());
                targetOwner.setAppUser(user);
            }

            // 3. Always ensure they hold the global Owner role if your system treats roles as global
            targetOwner.setRole("Owner");

            // 4. Safely synchronize bidirectional links without duplicate references
            if (targetOwner.getSurveys() == null) {
                targetOwner.setSurveys(new java.util.ArrayList<>());
            }

            if (!targetOwner.getSurveys().contains(survey)) {
                targetOwner.getSurveys().add(survey);
            }
            if (!survey.getReviewers().contains(targetOwner)) {
                survey.getReviewers().add(targetOwner);
            }

            // 5. Save ONLY the owning/cascading side of the relationship
            reviewerRepository.save(targetOwner);

        } else {
            // Fallback: Handle unauthenticated requests safely
            String fallbackEmail = "owner@example.com";
            Reviewer fallbackOwner = reviewerRepository.findFirstByEmail(fallbackEmail)
                    .orElseGet(() -> {
                        Reviewer f = new Reviewer();
                        f.setExternalId(java.util.UUID.randomUUID().toString());
                        f.setName("Survey Owner");
                        f.setEmail(fallbackEmail);
                        f.setAddedDate(Instant.now());
                        return f;
                    });

            fallbackOwner.setRole("Owner");

            if (fallbackOwner.getSurveys() == null) {
                fallbackOwner.setSurveys(new java.util.ArrayList<>());
            }

            if (!fallbackOwner.getSurveys().contains(survey)) {
                fallbackOwner.getSurveys().add(survey);
            }
            if (!survey.getReviewers().contains(fallbackOwner)) {
                survey.getReviewers().add(fallbackOwner);
            }

            reviewerRepository.save(fallbackOwner);
        }
        // No fallback for unauthenticated requests — survey creation requires auth
    }

    private SurveyDto toSurveyDto(Survey survey) {
        String extId = survey == null ? null : survey.getExternalId();
        List<Article> articles = survey == null ? List.of() : articleRepository.findBySurveyLinks_Survey(survey);
        int articleCount = articles == null ? 0 : articles.size();
        return new SurveyDto(
                extId,
                survey != null && survey.getTitle() != null ? survey.getTitle() : "Untitled Survey",
                survey != null ? survey.getDescription() : null,
                survey != null ? safeDate(survey.getCreatedDate()) : null,
                survey != null && survey.getStatus() != null ? survey.getStatus() : "Draft",
                articleCount
        );
    }

    private SurveyDetailsDto toSurveyDetailsDto(Survey survey) {
        List<Article> articles = articleRepository.findBySurveyExternalId(survey.getExternalId());
        int screened = (int) articles.stream().filter(article -> "INCLUDED".equals(article.getStatus()) || "EXCLUDED".equals(article.getStatus())).count();
        int pending = (int) articles.stream().filter(article -> "PENDING".equals(article.getStatus())).count();
        Reviewer ownerReviewer = reviewerRepository.findBySurveysContaining(survey).stream()
                .filter(reviewer -> "Owner".equals(reviewer.getRole()))
                .findFirst()
                .orElse(null);
        UserResponse owner = ownerReviewer == null
                ? new UserResponse(-1L, "Survey Owner", "owner@example.com")
                : new UserResponse(ownerReviewer.getReviewerId(), ownerReviewer.getName(), ownerReviewer.getEmail());

        return new SurveyDetailsDto(
                survey.getExternalId(),
                survey.getTitle(),
                survey.getDescription(),
                survey.getResearchQuestion(),
                safeDate(survey.getCreatedDate()),
                survey.getStatus(),
                articles.size(),
                screened,
                pending,
                owner
        );
    }

    public ArticleDto toArticleDto(Article article) {
        boolean hasPdf = article.getDocuments() != null && article.getDocuments().stream()
                .anyMatch(document -> document.getType() == DocumentType.PDF
                        && ((document.getFilePath() != null && !document.getFilePath().isBlank())
                        || (document.getPdfContent() != null && document.getPdfContent().length > 0)));
        String pdfUrl = hasPdf ? "/api/surveys/articles/" + article.getExternalId() + "/pdf" : null;
        String doiUrl = hasText(article.getDoi()) ? "https://doi.org/" + normalizeDoi(article.getDoi()) : null;
        String candidateExternalUrl = normalizeExternalUrl(article.getUrl());
        String externalUrl = isDoiResolverUrl(candidateExternalUrl, article.getDoi()) ? null : candidateExternalUrl;
        String scholarlySearchUrl = !hasPdf && externalUrl == null && doiUrl == null && hasText(article.getTitle())
                ? "https://scholar.google.com/scholar?q=" + URLEncoder.encode(article.getTitle(), StandardCharsets.UTF_8)
                : null;
        String openUrl = hasPdf ? pdfUrl : externalUrl != null ? externalUrl : doiUrl != null ? doiUrl : scholarlySearchUrl;
        String openType = hasPdf ? "pdf" : externalUrl != null ? "external" : doiUrl != null ? "doi"
                : scholarlySearchUrl != null ? "external" : null;
        Review latestReview = article.getArticleId() == null ? null
                : reviewRepository.findTopByArticle_ArticleIdOrderByReviewIdDesc(article.getArticleId()).orElse(null);
        String reviewedBy = latestReview != null && latestReview.getReviewer() != null ? latestReview.getReviewer().getName() : null;
        String reviewText = latestReview != null ? reviewDisplayText(latestReview.getJsonResponse()) : null;

        return new ArticleDto(
                article.getExternalId(),
                article.getTitle(),
                article.getAuthors() == null ? List.of() : article.getAuthors().stream().map(Author::getAuthorName).toList(),
                article.getJournal(),
                article.getPublicationYear(),
                article.getDoi(),
                article.getStatus(),
                article.getArticleAbstract(),
                article.getInclusionSummary(),
                article.getAddedById() == null ? null : new AddedByDto(article.getAddedById(), article.getAddedByName(), article.getAddedByRole()),
                externalUrl,
                openUrl,
                openType,
                hasPdf,
                pdfUrl,
                reviewedBy,
                reviewText
        );
    }

    private String reviewDisplayText(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return null;
        }
        try {
            java.util.Map<?, ?> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonResponse, java.util.Map.class);
            Object note = map.get("note");
            if (note != null && !note.toString().isBlank()) {
                return note.toString();
            }
            Object decision = map.get("decision");
            if (decision != null && !decision.toString().isBlank()) {
                return decision.toString();
            }
        } catch (Exception ignored) {
        }
        return jsonResponse;
    }

    private boolean isDoiResolverUrl(String url, String doi) {
        if (!hasText(url) || !hasText(doi)) {
            return false;
        }
        String normalizedUrl = url.toLowerCase(Locale.ROOT)
                .replace("http://dx.doi.org/", "")
                .replace("https://dx.doi.org/", "")
                .replace("http://doi.org/", "")
                .replace("https://doi.org/", "");
        return normalizeDoi(normalizedUrl).equals(normalizeDoi(doi));
    }

    public ReviewerDto toReviewerDto(Reviewer reviewer) {
        return new ReviewerDto(
                reviewer.getExternalId(),
                reviewer.getName(),
                reviewer.getEmail(),
                reviewer.getRole(),
                reviewer.getAddedDate() != null ? reviewer.getAddedDate().toString() : null
        );
    }

    private void attachPdfDocumentIfPresent(Article article, ArticleMetadata metadata, String fallbackFileName) {
        byte[] pdfBytes = metadata == null ? null : metadata.getPdfBytes();
        if (pdfBytes == null || pdfBytes.length == 0) {
            return;
        }

        String safeFileName = safeDownloadFileName(fallbackFileName, article.getExternalId());
        Document document = new Document();
        document.setTitle(defaultString(metadata.getTitle(), safeFileName));
        document.setType(DocumentType.PDF);
        document.setFilePath(storePdfFile(pdfBytes, article.getExternalId(), safeFileName));
        document.setExtractedText(defaultString(metadata.getAbstractText(), null));
        document.setMimeType(MediaType.APPLICATION_PDF_VALUE);
        document.setOriginalFileName(safeFileName);
        document.setChecksumSha256(sha256(pdfBytes));
        document.setArticle(article);
        article.getDocuments().add(document);
    }

    private void validatePdf(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF file is empty");
        }
        if (bytes.length > maxPdfSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF exceeds the configured upload limit");
        }
        if (mimeType != null && !mimeType.isBlank() && !MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(mimeType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only application/pdf files are accepted");
        }
        if (bytes.length < 5
                || bytes[0] != '%'
                || bytes[1] != 'P'
                || bytes[2] != 'D'
                || bytes[3] != 'F'
                || bytes[4] != '-') {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is not a valid PDF");
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to checksum PDF", e);
        }
    }

    private String safeDownloadFileName(String requestedName, String articleId) {
        String safe = sanitizeFileName(defaultString(requestedName, articleId + ".pdf"));
        if (safe.isBlank()) {
            safe = articleId + ".pdf";
        }
        if (!safe.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            safe += ".pdf";
        }
        return safe;
    }

    private String storePdfFile(byte[] bytes, String articleId, String requestedName) {
        try {
            Path storageDir = ensurePdfStorageDir();
            String checksum = sha256(bytes);
            String baseName = sanitizeFileName(defaultString(requestedName, articleId + ".pdf"));
            if (baseName.isBlank()) {
                baseName = articleId + ".pdf";
            }

            String fileName = articleId + "-" + checksum.substring(0, 12) + "-" + baseName;
            Path target = storageDir.resolve(fileName).normalize();
            if (!target.startsWith(storageDir)) {
                throw new IllegalStateException("Resolved PDF path escapes storage directory");
            }

            Files.write(target, bytes);
            return fileName;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to store PDF on disk", e);
        }
    }

    private byte[] readPdfBytes(Document document, Article article) {
        if (document == null) {
            throw new EntityNotFoundException("PDF not found for article: " + article.getExternalId());
        }

        if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
            try {
                Path resolved = resolveStoredPdfPath(document.getFilePath());
                if (!Files.exists(resolved)) {
                    throw new EntityNotFoundException("PDF not found for article: " + article.getExternalId());
                }
                return Files.readAllBytes(resolved);
            } catch (EntityNotFoundException e) {
                throw e;
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF could not be opened", e);
            }
        }

        if (document.getPdfContent() != null && document.getPdfContent().length > 0) {
            return document.getPdfContent();
        }

        throw new EntityNotFoundException("PDF not found for article: " + article.getExternalId());
    }

    private Path ensurePdfStorageDir() throws Exception {
        Path storageDir = Path.of(paperStorageDir).toAbsolutePath().normalize();
        Files.createDirectories(storageDir);
        return storageDir;
    }

    private Path resolveStoredPdfPath(String relativePath) throws Exception {
        Path storageDir = ensurePdfStorageDir();
        Path resolved = storageDir.resolve(relativePath).normalize();
        if (!resolved.startsWith(storageDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PDF path");
        }
        if (Files.exists(resolved)) {
            return resolved;
        }

        for (Path legacyBase : legacyPdfStorageBases()) {
            Path legacyResolved = legacyBase.resolve(relativePath).normalize();
            if (legacyResolved.startsWith(legacyBase) && Files.exists(legacyResolved)) {
                return legacyResolved;
            }
        }
        return resolved;
    }

    private List<Path> legacyPdfStorageBases() {
        return List.of(
                Path.of(System.getProperty("user.home"), ".literature-review-assistant", "papers").toAbsolutePath().normalize(),
                Path.of("/app/uploads/papers").toAbsolutePath().normalize()
        );
    }

    private void deleteStoredFiles(Article article) {
        if (article == null || article.getDocuments() == null) {
            return;
        }

        for (Document document : article.getDocuments()) {
            if (document.getFilePath() == null || document.getFilePath().isBlank()) {
                continue;
            }

            try {
                Files.deleteIfExists(resolveStoredPdfPath(document.getFilePath()));
            } catch (Exception e) {
                System.err.println("Failed to delete stored file " + document.getFilePath() + ": " + e.getMessage());
            }
        }
    }

    private AddedByDto resolveAddedBy(Survey survey, AddedByDto provided, String authorizationHeader) {
        Optional<AppUser> current = currentUser(authorizationHeader);
        if (current.isPresent()) {
            AppUser user = current.get();
            boolean owner = reviewerRepository.findBySurveysContaining(survey).stream()
                    .anyMatch(reviewer -> "Owner".equals(reviewer.getRole()) && user.getEmail().equalsIgnoreCase(reviewer.getEmail()));
            return new AddedByDto(
                    user.getId() == null ? user.getEmail() : user.getId().toString(),
                    user.getName(),
                    owner ? "Owner" : "Reviewer"
            );
        }

        if (provided != null) {
            return provided;
        }

        return new AddedByDto("owner", "Survey Owner", "Reviewer");
    }

    private void ensureOwnerAccess(Survey survey, String authorizationHeader) {
        AppUser current = currentUserRequired(authorizationHeader);
        if (!isCurrentUserSurveyOwner(survey, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the survey owner can manage reviewers");
        }
    }

    private boolean canEditArticle(Survey survey, Article article, AppUser currentUser) {
        if (isCurrentUserSurveyOwner(survey, currentUser)) {
            return true;
        }

        String currentUserId = currentUser.getId() == null ? null : currentUser.getId().toString();
        return equalsAnyIgnoreCase(article.getAddedById(), currentUserId, currentUser.getEmail());
    }

    private boolean canAccessArticle(Article article, AppUser currentUser) {
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return true;
        }

        return article.getSurveyLinks().stream()
                .map(ArticleSurvey::getSurvey)
                .flatMap(survey -> reviewerRepository.findBySurveysContaining(survey).stream())
                .anyMatch(reviewer -> reviewer.getEmail() != null
                        && currentUser.getEmail() != null
                        && reviewer.getEmail().equalsIgnoreCase(currentUser.getEmail()));
    }

    private boolean isCurrentUserSurveyOwner(Survey survey, AppUser currentUser) {
        Reviewer owner = reviewerRepository.findBySurveysContaining(survey).stream()
                .filter(reviewer -> "Owner".equals(reviewer.getRole()))
                .findFirst()
                .orElse(null);

        return owner != null
                && owner.getEmail() != null
                && currentUser.getEmail() != null
                && owner.getEmail().equalsIgnoreCase(currentUser.getEmail());
    }

    private boolean equalsAnyIgnoreCase(String value, String... candidates) {
        if (value == null) {
            return false;
        }

        for (String candidate : candidates) {
            if (candidate != null && value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private Optional<AppUser> currentUser(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token.isBlank()) {
            return Optional.empty();
        }

        return authSessionRepository.findByToken(token).map(AuthSession::getUser);
    }

    private AppUser currentUserRequired(String authorizationHeader) {
        return currentUser(authorizationHeader)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return "";
        }

        // Handles case-insensitive "bearer " tokens and trims any stray spaces
        if (authorizationHeader.toLowerCase().startsWith("bearer ")) {
            return authorizationHeader.substring(7).trim();
        }

        return authorizationHeader.trim();
    }

    private byte[] decodePdfBytes(String input) {
        if (input == null || input.isBlank()) {
            return new byte[0];
        }

        String data = input.trim();
        int comma = data.indexOf(',');
        if (data.startsWith("data:") && comma >= 0) {
            data = data.substring(comma + 1);
        }

        try {
            return Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PDF payload", e);
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName
                .replaceAll("[\\\\/:*?\"<>|]+", "_")
                .replace("..", "_")
                .replaceAll("[\\p{Cntrl}]+", "")
                .trim();
    }

    private String buildSurveyContext(Survey survey, List<Article> articles) {
        StringBuilder builder = new StringBuilder();
        builder.append("Survey description: ").append(defaultString(survey.getDescription(), "")).append("\n");
        builder.append("Articles:\n");

        int limit = Math.min(articles.size(), 10);
        for (int i = 0; i < limit; i++) {
            Article article = articles.get(i);
            builder.append(i + 1).append(". ");
            builder.append(defaultString(article.getTitle(), "Untitled Article")).append(" | ");
            builder.append("Authors: ").append(article.getAuthors() == null ? "" : article.getAuthors().stream().map(Author::getAuthorName).reduce((a, b) -> a + ", " + b).orElse("")).append(" | ");
            builder.append("Journal: ").append(defaultString(article.getJournal(), "")).append(" | ");
            builder.append("Year: ").append(article.getPublicationYear() == null ? "" : article.getPublicationYear()).append("\n");
            builder.append("Abstract: ").append(defaultString(article.getArticleAbstract(), "")).append("\n\n");
        }

        return builder.toString();
    }

    private String safeDate(LocalDate date) {
        return (date == null ? LocalDate.now() : date).toString();
    }


}
