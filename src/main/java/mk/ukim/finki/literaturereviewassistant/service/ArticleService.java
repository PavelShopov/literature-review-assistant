package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Author;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ArticleService {

        // ─── CRUD ────────────────────────────────────────────────────────────────

        List<Article> findAll();

        Optional<Article> findById(Long articleId);

        Optional<Article> findByDoi(String doi);

        Optional<Article> findByUrl(String url);

        Article save(Article article);

        void deleteById(Long articleId);

        // ─── Import / Ingestion ───────────────────────────────────────────────────

        /**
         * @param bibFile the uploaded .bib file
         * @return list of newly created Articles
         */
        List<Article> importFromBib(MultipartFile bibFile);

        /**
         * Fetch article metadata + full-text PDF by downloading from the given URL,
         * then persists the Article and its associated Document.
         *
         * @param url publicly accessible article URL
         * @return the created Article
         */
        Article importFromUrl(String url);

        /**
         * Upload a PDF directly; metadata (title, doi, abstract) is extracted
         * from the document text and a Document entity is attached.
         *
         * @param pdfFile uploaded PDF
         * @return the created Article
         */
        Article uploadPdf(MultipartFile pdfFile);

        /**
         * Search Semantic Scholar via its public API and import matching articles.
         *
         * @param query    search keywords
         * @param maxResults maximum number of results to import
         * @return list of imported Articles
         */
        List<Article> importFromSemanticScholar(String query, int maxResults);

        // ─── Survey Association ───────────────────────────────────────────────────

        /**
         * Attach an article to a survey (PRISMA inclusion).
         */
        Article addToSurvey(Long articleId, Long surveyId);

        /**
         * Remove an article from a survey (PRISMA exclusion).
         */
        Article removeFromSurvey(Long articleId, Long surveyId);

        /**
         * Return all articles belonging to a survey.
         */
        List<Article> findBySurvey(Long surveyId);

        /**
         * Filter articles in a survey by keyword match in title or abstract.
         */
        List<Article> searchInSurvey(Long surveyId, String keyword);

        // ─── Author Management ────────────────────────────────────────────────────

        List<Author> findAuthorsByArticle(Long articleId);

        Article addAuthor(Long articleId, Long authorId);

        Article removeAuthor(Long articleId, Long authorId);

        Map<String, Object> annotate(Long articleId, Long promptId, boolean useFullText);

        Map<String, Object> ask(Long articleId, Long promptId, boolean useFullText);

        Map<Long, Map<String, Object>> batchAnnotate(Long surveyId, Long promptId, boolean useFullText);

        Map<Long, Map<String, Object>> batchAsk(Long surveyId, Long promptId, boolean useFullText);

        // ─── AI Annotation ───────────────────────────────────────────────────────

//        /**
//         * Run a structured annotation prompt over the article's abstract and/or
//         * full text. Returns a JSON map keyed by the fields defined in the Prompt.
//         *
//         * @param articleId  target article
//         * @param promptId   which Prompt template to use
//         * @param useFullText if true, full document text is included; otherwise
//         *                    only the abstract is sent
//         * @return structured JSON annotation result (stored as AnnotationResult)
//         */
//        Map<String, Object> annotate(Long articleId, Long promptId, boolean useFullText);
//
//        /**
//         * Ask a yes/no inclusion question about the article using the given Prompt.
//         * Returns a map with keys: "answer" (true/false) and "explanation" (String).
//         *
//         * @param articleId  target article
//         * @param promptId   which Prompt template to use (should be a boolean prompt)
//         * @param useFullText if true, full text is included; otherwise abstract only
//         * @return map with "answer" (Boolean) and "explanation" (String)
//         */
//        Map<String, Object> ask(Long articleId, Long promptId, boolean useFullText);
//
//        /**
//         * Batch-annotate all articles in a survey using the same prompt template.
//         *
//         * @param surveyId    survey whose articles will be annotated
//         * @param promptId    prompt template id
//         * @param useFullText whether to use full text or abstract only
//         * @return map of articleId → annotation result
//         */
//        Map<Long, Map<String, Object>> batchAnnotate(Long surveyId, Long promptId, boolean useFullText);
//
//        /**
//         * Batch-ask inclusion/exclusion question for all articles in a survey.
//         *
//         * @param surveyId    survey whose articles will be screened
//         * @param promptId    boolean prompt template id
//         * @param useFullText whether to use full text or abstract only
//         * @return map of articleId → {answer, explanation}
//         */
//        Map<Long, Map<String, Object>> batchAsk(Long surveyId, Long promptId, boolean useFullText);

        /**
         * Uses article.py to fetch abstracts for articles that don't have one.
         */
        int syncAbstracts();
}


