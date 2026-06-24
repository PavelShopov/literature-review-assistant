package mk.ukim.finki.literaturereviewassistant.service.impl;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.literaturereviewassistant.model.*;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleRepository;
import mk.ukim.finki.literaturereviewassistant.repository.AuthorRepository;
import mk.ukim.finki.literaturereviewassistant.repository.AuthSessionRepository;
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
import mk.ukim.finki.literaturereviewassistant.web.dto.ReviewerDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDetailsDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyAskRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyRequest;
import mk.ukim.finki.literaturereviewassistant.web.dto.UserResponse;
import org.springframework.security.core.userdetails.UserDetails;
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

import java.time.Instant;
import java.time.LocalDate;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SurveyServiceImpl implements SurveyService {

    private final SurveyRepository surveyRepository;
    private final ArticleRepository articleRepository;
    private final AuthorRepository authorRepository;
    private final AuthSessionRepository authSessionRepository;
    private final ReviewerRepository reviewerRepository;
    private final BibTexParser bibTexParser;
    private final PdfExtractorService pdfExtractorService;
    private final RestTemplate restTemplate;
    private final GeminiService geminiService;
    private final GemmaService gemmaService;

    public SurveyServiceImpl(
            SurveyRepository surveyRepository,
            ArticleRepository articleRepository,
            AuthorRepository authorRepository,
            AuthSessionRepository authSessionRepository,
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
        this.reviewerRepository = reviewerRepository;
        this.bibTexParser = bibTexParser;
        this.pdfExtractorService = pdfExtractorService;
        this.restTemplate = restTemplate;
        this.geminiService = geminiService;
        this.gemmaService = gemmaService;
    }


//    @Override
//    @Transactional(readOnly = true)
//    public List<SurveyDto> findAllSurveys(String authorizationHeader) {
//        Optional<AppUser> currentUser = currentUser(authorizationHeader);
//        if (currentUser.isEmpty()) {
//            return List.of();
//        }
//
//        AppUser user = currentUser.get();
//        if ("ADMIN".equals(user.getRole())) {
//            return surveyRepository.findAll().stream().map(this::toSurveyDto).toList();
//        }
//// <<<<<<< sandbox_combined
//
//        return surveyRepository.findAll().stream()
//                .filter(survey -> survey.getReviewers().stream()
//                        .anyMatch(contributor -> contributor.getEmail() != null
//                                && user.getEmail() != null
//                                && contributor.getEmail().equalsIgnoreCase(user.getEmail())))
//// =======
////         return surveyRepository.findAll().stream()
////                 .filter(survey -> survey.getReviewers().stream()
////                         .anyMatch(contributor -> contributor.getEmail().equals(currentUser.get().getEmail())))
//// >>>>>>> sandbox_branch
//                .map(this::toSurveyDto)
//                .toList();
//    }

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

        // 3. Bind the owner profile before storing
        ensureOwner(survey, authorizationHeader);

        // 4. Save the completed entity structure
        Survey savedSurvey = surveyRepository.save(survey);

        // 5. Explicitly break out early if it's a creation step to bypass proxy execution loops
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
        getSurveyOrThrow(surveyId);
        return articleRepository.findBySurveyExternalId(surveyId).stream().map(this::toArticleDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleDto findArticle(String articleId) {
        return toArticleDto(getArticleOrThrow(articleId));
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
                imported.add(saveImportedArticle(survey, addedBy, metadataFromBibEntry(entry), null));
            }
            if (imported.isEmpty()) {
                throw new EntityNotFoundException("No BibTeX entries could be parsed");
            }
            return toArticleDto(imported.get(0));
        }

        if ("pdf".equalsIgnoreCase(request.type())) {
            byte[] pdfBytes = decodePdfBytes(request.data());
            if (pdfBytes.length == 0) {
                throw new IllegalArgumentException("PDF data is empty");
            }

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
        ArticleMetadata metadata = pdfExtractorService.extractFromBytes(readBytes(file), file.getOriginalFilename());
        return toArticleDto(saveImportedArticle(survey, author, metadata, file.getOriginalFilename()));
    }

    @Override
    @Transactional
    public ArticleDto updateArticle(String surveyId, String articleId, ArticleUpdateRequest request, String authorizationHeader) {
        Survey survey = getSurveyOrThrow(surveyId);
        Article article = articleRepository.findByExternalId(articleId)
                .filter(item -> surveyId.equals(item.getSurveyExternalId()))
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
            article.setDoi(request.doi().isBlank() ? null : request.doi().trim());
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
                .filter(item -> surveyId.equals(item.getSurveyExternalId()))
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
        metadata.setDoi(entry.getDoi());
        metadata.setUrl(entry.getUrl());
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
        String doi = extractDoi(rawInput);
        if (doi == null || doi.isBlank()) {
            ArticleMetadata fallback = new ArticleMetadata();
            fallback.setTitle(extractTitleFromUrl(rawInput));
            fallback.setDoi(rawInput);
            return fallback;
        }

        ArticleMetadata bibtex = fetchDoiBibtexMetadata(doi);
        if (isMetadataUsable(bibtex)) {
            return bibtex;
        }

        ArticleMetadata crossref = fetchCrossrefMetadata(doi);
        if (isMetadataUsable(crossref)) {
            return crossref;
        }

        ArticleMetadata doiOrg = fetchDoiOrgMetadata(doi);
        if (isMetadataUsable(doiOrg)) {
            return doiOrg;
        }

        ArticleMetadata landingPage = fetchLandingPageMetadata(doi);
        if (isMetadataUsable(landingPage)) {
            return landingPage;
        }

        ArticleMetadata fallback = new ArticleMetadata();
        fallback.setDoi(doi);
        fallback.setTitle(extractTitleFromDoi(doi));
        return fallback;
    }

    private ArticleMetadata fetchDoiBibtexMetadata(String doi) {
        String encoded = URLEncoder.encode(doi, StandardCharsets.UTF_8);
        String apiUrl = "https://doi.org/" + encoded;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.valueOf("application/x-bibtex")));
            headers.set(HttpHeaders.USER_AGENT, "literature-review-assistant/1.0 (mailto:unknown@example.com)");

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
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
        String encoded = URLEncoder.encode(doi, StandardCharsets.UTF_8);
        String apiUrl = "https://api.crossref.org/works/" + encoded;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set(HttpHeaders.USER_AGENT, "literature-review-assistant/1.0 (mailto:unknown@example.com)");

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
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

    private ArticleMetadata fetchDoiOrgMetadata(String doi) {
        String encoded = URLEncoder.encode(doi, StandardCharsets.UTF_8);
        String apiUrl = "https://doi.org/" + encoded;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.valueOf("application/vnd.citationstyles.csl+json")));
            headers.set(HttpHeaders.USER_AGENT, "literature-review-assistant/1.0 (mailto:unknown@example.com)");

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
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
        metadata.setDoi(stringValue(message.getOrDefault("DOI", doi)));
        metadata.setTitle(firstTextValue(message.get("title")));
        metadata.setAbstractText(cleanCrossrefAbstract(stringValue(message.get("abstract"))));
        metadata.setUrl(stringValue(message.get("URL")));
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
        metadata.setDoi(defaultString(stringValue(message.getOrDefault("DOI", doi)), doi));
        metadata.setTitle(firstTextValue(message.get("title")));
        metadata.setAbstractText(cleanCrossrefAbstract(stringValue(message.get("abstract"))));
        metadata.setUrl(stringValue(message.get("URL")));
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
        String apiUrl = "https://doi.org/" + URLEncoder.encode(doi, StandardCharsets.UTF_8);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML));
            headers.set(HttpHeaders.USER_AGENT, "literature-review-assistant/1.0 (mailto:unknown@example.com)");

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getBody() != null) {
                return metadataFromHtml(response.getBody(), doi, apiUrl);
            }
        } catch (Exception e) {
            System.err.println("DOI landing page lookup failed for DOI " + doi + ": " + e.getMessage());
        }

        return new ArticleMetadata();
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
        // 1. Double check if the article already exists by DOI to prevent duplication
        if (metadata.getDoi() != null && !metadata.getDoi().isBlank()) {
            Optional<Article> existing = articleRepository.findByDoi(metadata.getDoi());
            if (existing.isPresent()) {
                Article existingArticle = existing.get();

                // Link existing article to the new survey if not linked already
                boolean alreadyLinked = existingArticle.getSurveyLinks().stream()
                        .anyMatch(link -> link.getSurvey().getSurveyId().equals(survey.getSurveyId()));

                if (!alreadyLinked) {
                    ArticleSurvey link = new ArticleSurvey();
                    link.setArticle(existingArticle);
                    link.setSurvey(survey);
                    link.setStatus(mk.ukim.finki.literaturereviewassistant.model.ArticleStatus.INCLUDED);

                    existingArticle.getSurveyLinks().add(link);
                    survey.getArticleLinks().add(link);

                    return articleRepository.save(existingArticle);
                }
                return existingArticle;
            }
        }

        // 2. Build the new Article entity
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean looksLikeDoi(String input) {
        String lower = input.toLowerCase();
        return input.startsWith("10.") || lower.contains("doi.org/") || lower.startsWith("doi:");
    }

    private String extractDoi(String input) {
        String trimmed = input.trim();
        if (trimmed.toLowerCase().startsWith("doi:")) {
            trimmed = trimmed.substring(4).trim();
        }
        int doiIndex = trimmed.toLowerCase().indexOf("doi.org/");
        if (doiIndex >= 0) {
            trimmed = trimmed.substring(doiIndex + "doi.org/".length());
        }
        trimmed = trimmed.replaceAll("[?#].*$", "").trim();
        try {
            return URLDecoder.decode(trimmed, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return trimmed;
        }
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
        // For non-admin users: only show surveys where they are the Owner.
        // Surveys they are assigned to review (Reviewer role) are available via /api/reviews/surveys.
        return surveyRepository.findAll().stream()
                .filter(survey -> survey.getReviewers().stream()
                        .anyMatch(contributor ->
                                contributor.getEmail().equals(currentUser.get().getEmail())
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

// <<<<<<< sandbox_combined
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
// >>>>>>> sandbox_branch
        }
        // No fallback for unauthenticated requests — survey creation requires auth
    }

    private SurveyDto toSurveyDto(Survey survey) {
        if (survey == null) return null;

        int articleCount = 0;
        String extId = survey.getExternalId();

        // Safely check if articles exist without breaking the session proxy
        if (extId != null && !extId.isBlank()) {
            // If your Survey entity has @OneToMany List<ArticleSurvey> articleLinks, use that instead to avoid repository overhead:
            // articleCount = survey.getArticleLinks() != null ? survey.getArticleLinks().size() : 0;

            // Otherwise, use this safe defensive repository fallback:
            List<Article> articles = articleRepository.findBySurveyExternalId(extId);
            if (articles != null) {
                articleCount = articles.size();
            }
        }

        return new SurveyDto(
                extId,
                survey.getTitle() != null ? survey.getTitle() : "Untitled Survey",
                survey.getDescription(),
                safeDate(survey.getCreatedDate()),
                survey.getStatus() != null ? survey.getStatus() : "Draft",
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
                article.getAddedById() == null ? null : new AddedByDto(article.getAddedById(), article.getAddedByName(), article.getAddedByRole())
        );
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

        try {
            Path directory = Path.of("storage", "pdfs", article.getSurveyExternalId());
            Files.createDirectories(directory);

            String safeFileName = sanitizeFileName(defaultString(fallbackFileName, article.getExternalId() + ".pdf"));
            if (!safeFileName.toLowerCase().endsWith(".pdf")) {
                safeFileName = safeFileName + ".pdf";
            }

            Path filePath = directory.resolve(article.getExternalId() + "-" + safeFileName);
            Files.write(filePath, pdfBytes);

            Document document = new Document();
            document.setTitle(defaultString(metadata.getTitle(), safeFileName));
            document.setType(DocumentType.PDF);
            document.setFilePath(filePath.toString());
            document.setExtractedText(defaultString(metadata.getAbstractText(), null));
            document.setArticle(article);
            article.getDocuments().add(document);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store uploaded PDF", e);
        }
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
                Files.deleteIfExists(Path.of(document.getFilePath()));
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
        return fileName.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
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
