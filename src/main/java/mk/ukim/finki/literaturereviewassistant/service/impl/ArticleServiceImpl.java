package mk.ukim.finki.literaturereviewassistant.service.impl;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.literaturereviewassistant.model.*;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleRepository;
import mk.ukim.finki.literaturereviewassistant.repository.AuthorRepository;
import mk.ukim.finki.literaturereviewassistant.repository.DocumentRepository;
import mk.ukim.finki.literaturereviewassistant.repository.PromptRepository;
import mk.ukim.finki.literaturereviewassistant.repository.SurveyRepository;
import mk.ukim.finki.literaturereviewassistant.service.AIServices.LlmAnnotationService;
import mk.ukim.finki.literaturereviewassistant.service.ArticleService;
import mk.ukim.finki.literaturereviewassistant.service.DataService.BibEntry;
import mk.ukim.finki.literaturereviewassistant.service.DataService.BibTexParser;
import mk.ukim.finki.literaturereviewassistant.service.DataService.PdfExtractorService;
import mk.ukim.finki.literaturereviewassistant.service.DataService.ArticleMetadata;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleServiceImpl implements ArticleService{

    private final ArticleRepository articleRepository;
    private final AuthorRepository authorRepository;
    private final SurveyRepository surveyRepository;
    private final DocumentRepository documentRepository;
    private final PromptRepository promptRepository;

    // ─── External / AI clients (inject your own implementations) ─────────────
    private final BibTexParser bibTexParser;          // parses .bib files
    private final PdfExtractorService pdfExtractorService;   // extracts text / metadata from PDFs
    private final SemanticScholarClient semanticScholarClient; // calls the Semantic Scholar API
    private final LlmAnnotationService  llmAnnotationService;  // wraps the LLM (e.g. Claude / GPT)

    @Override
    @Transactional(readOnly = true)
    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Article> findById(Long articleId) {
        return articleRepository.findById(articleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Article> findByDoi(String doi) {
        return articleRepository.findByDoi(doi);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Article> findByUrl(String url) {
        return articleRepository.findByUrl(url);
    }

    @Override
    public Article save(Article article) {
        return articleRepository.save(article);
    }

    @Override
    public void deleteById(Long articleId) {
        articleRepository.deleteById(articleId);
    }

    // =========================================================================
    // Import / Ingestion
    // =========================================================================

    @Override
    public List<Article> importFromBib(MultipartFile bibFile) {
        List<BibEntry> entries = bibTexParser.parse(bibFile);
        List<Article> imported = new ArrayList<>();

        for (BibEntry entry : entries) {
            // Skip duplicates by DOI
            if (entry.getDoi() != null &&
                    articleRepository.findByDoi(entry.getDoi()).isPresent()) {
                continue;
            }

            Article article = new Article();
            article.setTitle(entry.getTitle());
            article.setDoi(entry.getDoi());
            article.setUrl(entry.getUrl());
            article.setArticleAbstract(entry.getAbstract());
            article.setAuthors(resolveOrCreateAuthors(entry.getAuthorNames()));

            imported.add(articleRepository.save(article));
        }
        return imported;
    }

    @Override
    public Article importFromUrl(String url) {
        // Return existing article if already imported
        return articleRepository.findByUrl(url).orElseGet(() -> {
            ArticleMetadata meta = pdfExtractorService.extractFromUrl(url);

            Article article = new Article();
            article.setUrl(url);
            article.setTitle(meta.getTitle());
            article.setDoi(meta.getDoi());
            article.setArticleAbstract(meta.getAbstractText());
            article.setAuthors(resolveOrCreateAuthors(meta.getAuthorNames()));

            Article saved = articleRepository.save(article);

            if (meta.getPdfBytes() != null) {
                attachDocument(saved, meta.getPdfBytes(), DocumentType.PDF);
            }
            return saved;
        });
    }

    @Override
    public Article uploadPdf(MultipartFile pdfFile) {
        byte[] bytes;
        try {
            bytes = pdfFile.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read uploaded PDF", e);
        }

        ArticleMetadata meta = pdfExtractorService.extractFromBytes(bytes);

        // Deduplicate by DOI if available
        if (meta.getDoi() != null) {
            Optional<Article> existing = articleRepository.findByDoi(meta.getDoi());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Article article = new Article();
        article.setTitle(meta.getTitle());
        article.setDoi(meta.getDoi());
        article.setArticleAbstract(meta.getAbstractText());
        article.setAuthors(resolveOrCreateAuthors(meta.getAuthorNames()));

        Article saved = articleRepository.save(article);

        attachDocument(saved, bytes, DocumentType.PDF);
        return saved;
    }

    @Override
    public List<Article> importFromSemanticScholar(String query, int maxResults) {
        return List.of();
    }

//    @Override
//    public List<Article> importFromSemanticScholar(String query, int maxResults) {
//        List<SemanticScholarResult> results =
//                semanticScholarClient.search(query, maxResults);
//
//        List<Article> imported = new ArrayList<>();
//        for (SemanticScholarResult r : results) {
//            if (r.getDoi() != null &&
//                    articleRepository.findByDoi(r.getDoi()).isPresent()) {
//                continue;
//            }
//              //TODO Add semantic scholar
//            Article article = new Article();
//            article.setTitle(r.getTitle());
//            article.setDoi(r.getDoi());
//            article.setUrl(r.getUrl());
//            article.setArticleAbstract(r.getAbstractText());
//            article.setAuthors(resolveOrCreateAuthors(r.getAuthorNames()));
//
//            imported.add(articleRepository.save(article));
//        }
//        return imported;
//    }

    // =========================================================================
    // Survey Association
    // =========================================================================

    @Override
    public Article addToSurvey(Long articleId, Long surveyId) {
        Article article = getArticleOrThrow(articleId);
        Survey survey  = getSurveyOrThrow(surveyId);

        if (!article.getSurveys().contains(survey)) {
            article.getSurveys().add(survey);
        }
        return articleRepository.save(article);
    }

    @Override
    public Article removeFromSurvey(Long articleId, Long surveyId) {
        Article article = getArticleOrThrow(articleId);
        Survey  survey  = getSurveyOrThrow(surveyId);

        article.getSurveys().remove(survey);
        return articleRepository.save(article);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Article> findBySurvey(Long surveyId) {
        Survey survey = getSurveyOrThrow(surveyId);
        return articleRepository.findBySurveysContaining(survey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Article> searchInSurvey(Long surveyId, String keyword) {
        Survey survey = getSurveyOrThrow(surveyId);
        String kw = keyword.toLowerCase();
        return articleRepository.findBySurveysContaining(survey).stream()
                .filter(a -> (a.getTitle() != null && a.getTitle().toLowerCase().contains(kw))
                        || (a.getArticleAbstract() != null && a.getArticleAbstract().toLowerCase().contains(kw)))
                .toList();
    }

    // =========================================================================
    // Author Management
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Author> findAuthorsByArticle(Long articleId) {
        return getArticleOrThrow(articleId).getAuthors();
    }

    @Override
    public Article addAuthor(Long articleId, Long authorId) {
        Article article = getArticleOrThrow(articleId);
        Author  author  = authorRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Author not found: " + authorId));

        if (!article.getAuthors().contains(author)) {
            article.getAuthors().add(author);
        }
        return articleRepository.save(article);
    }

    @Override
    public Article removeAuthor(Long articleId, Long authorId) {
        Article article = getArticleOrThrow(articleId);
        article.getAuthors().removeIf(a -> a.getAuthorId().equals(authorId));
        return articleRepository.save(article);
    }

    // =========================================================================
    // AI Annotation
    // =========================================================================

    @Override
    public Map<String, Object> annotate(Long articleId, Long promptId, boolean useFullText) {
        Article article = getArticleOrThrow(articleId);
        Prompt prompt  = getPromptOrThrow(promptId);

        String context = buildContext(article, useFullText);
        Map<String, Object> result = llmAnnotationService.annotate(context, prompt);

        // Persist the annotation result linked to the article
        llmAnnotationService.saveAnnotationResult(article, prompt, result);
        return result;
    }

    @Override
    public Map<String, Object> ask(Long articleId, Long promptId, boolean useFullText) {
        Article article = getArticleOrThrow(articleId);
        Prompt  prompt  = getPromptOrThrow(promptId);

        String context = buildContext(article, useFullText);

        // Expects LLM to return { "answer": true/false, "explanation": "..." }
        Map<String, Object> result = llmAnnotationService.ask(context, prompt);

        llmAnnotationService.saveAnnotationResult(article, prompt, result);
        return result;
    }

    @Override
    public Map<Long, Map<String, Object>> batchAnnotate(Long surveyId,
                                                        Long promptId,
                                                        boolean useFullText) {
        List<Article> articles = findBySurvey(surveyId);
        Map<Long, Map<String, Object>> results = new LinkedHashMap<>();
        for (Article article : articles) {
            results.put(article.getArticleId(),
                    annotate(article.getArticleId(), promptId, useFullText));
        }
        return results;
    }

    @Override
    public Map<Long, Map<String, Object>> batchAsk(Long surveyId,
                                                   Long promptId,
                                                   boolean useFullText) {
        List<Article> articles = findBySurvey(surveyId);
        Map<Long, Map<String, Object>> results = new LinkedHashMap<>();
        for (Article article : articles) {
            results.put(article.getArticleId(),
                    ask(article.getArticleId(), promptId, useFullText));
        }
        return results;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Article getArticleOrThrow(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("Article not found: " + articleId));
    }

    private Survey getSurveyOrThrow(Long surveyId) {
        return surveyRepository.findById(surveyId)
                .orElseThrow(() -> new EntityNotFoundException("Survey not found: " + surveyId));
    }

    private Prompt getPromptOrThrow(Long promptId) {
        return promptRepository.findById(promptId)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found: " + promptId));
    }

    /**
     * Resolve authors by name, creating new Author records for unknowns.
     */
    private List<Author> resolveOrCreateAuthors(List<String> authorNames) {
        if (authorNames == null) return new ArrayList<>();
        List<Author> authors = new ArrayList<>();
        for (String name : authorNames) {
            Author author = authorRepository.findByAuthorName(name)
                    .orElseGet(() -> {
                        Author a = new Author();
                        a.setAuthorName(name);
                        return authorRepository.save(a);
                    });
            authors.add(author);
        }
        return authors;
    }

    /**
     * Build the text context sent to the LLM.
     * If useFullText is true and a Document with extracted text exists, append it.
     */
    private String buildContext(Article article, boolean useFullText) {
        StringBuilder sb = new StringBuilder();

        if (article.getTitle() != null) {
            sb.append("Title: ").append(article.getTitle()).append("\n\n");
        }
        if (article.getArticleAbstract() != null) {
            sb.append("Abstract: ").append(article.getArticleAbstract()).append("\n\n");
        }
        if (useFullText && article.getDocuments() != null) {
            article.getDocuments().stream()
                    .filter(d -> d.getExtractedText() != null)
                    .findFirst()
                    .ifPresent(d -> sb.append("Full Text:\n").append(d.getExtractedText()));
        }
        return sb.toString().trim();
    }

    /**
     * Create and persist a Document entity linked to the given article.
     */
    private void attachDocument(Article article, byte[] content, DocumentType type) {
        Document doc = new Document();
        doc.setArticle(article);
        doc.setType(type);
//        doc.setContent(content);
//        doc.setExtractedText(pdfExtractorService.extractText(content));
        documentRepository.save(doc);
    }
}
