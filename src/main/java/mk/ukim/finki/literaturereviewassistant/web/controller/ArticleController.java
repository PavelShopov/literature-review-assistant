package mk.ukim.finki.literaturereviewassistant.web.controller;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Author;
import mk.ukim.finki.literaturereviewassistant.model.Review;
import mk.ukim.finki.literaturereviewassistant.service.ArticleService;
import mk.ukim.finki.literaturereviewassistant.service.GemmaService;

import mk.ukim.finki.literaturereviewassistant.service.ReviewService;
import mk.ukim.finki.literaturereviewassistant.web.dto.ReviewSubmissionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "http://localhost:5173")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private GemmaService gemmaService;

    // ─── CRUD ────────────────────────────────────────────────────────────────
    @GetMapping
    public List<Article> getAll() {
        return articleService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Article> getById(@PathVariable Long id) {
        return articleService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Article create(@RequestBody Article article) {
        return articleService.save(article);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Import / Ingestion ───────────────────────────────────────────────────
    @PostMapping("/import/bib")
    public List<Article> importFromBib(@RequestParam("file") MultipartFile bibFile) {
        return articleService.importFromBib(bibFile);
    }

    @PostMapping("/import/url")
    public Article importFromUrl(@RequestParam("url") String url) {
        return articleService.importFromUrl(url);
    }

    @PostMapping("/upload/pdf")
    public Article uploadPdf(@RequestParam("file") MultipartFile pdfFile) {
        return articleService.uploadPdf(pdfFile);
    }

    @GetMapping("/search/semantic")
    public List<Article> importFromSemanticScholar(@RequestParam String query,
                                                   @RequestParam(defaultValue = "10") int maxResults) {
        return articleService.importFromSemanticScholar(query, maxResults);
    }

    // ─── Survey Association ───────────────────────────────────────────────────
    @PostMapping("/{articleId}/survey/{surveyId}")
    public Article addToSurvey(@PathVariable Long articleId, @PathVariable Long surveyId) {
        return articleService.addToSurvey(articleId, surveyId);
    }

    @DeleteMapping("/{articleId}/survey/{surveyId}")
    public Article removeFromSurvey(@PathVariable Long articleId, @PathVariable Long surveyId) {
        return articleService.removeFromSurvey(articleId, surveyId);
    }

    @GetMapping("/survey/{surveyId}")
    public List<Article> getBySurvey(@PathVariable Long surveyId) {
        return articleService.findBySurvey(surveyId);
    }

    @GetMapping("/survey/{surveyId}/search")
    public List<Article> searchInSurvey(@PathVariable Long surveyId,
                                        @RequestParam String keyword) {
        return articleService.searchInSurvey(surveyId, keyword);
    }

    // ─── Author Management ───────────────────────────────────────────────────
    @GetMapping("/{articleId}/authors")
    public List<Author> getAuthors(@PathVariable Long articleId) {
        return articleService.findAuthorsByArticle(articleId);
    }

    @PostMapping("/{articleId}/author/{authorId}")
    public Article addAuthor(@PathVariable Long articleId, @PathVariable Long authorId) {
        return articleService.addAuthor(articleId, authorId);
    }

    @DeleteMapping("/{articleId}/author/{authorId}")
    public Article removeAuthor(@PathVariable Long articleId, @PathVariable Long authorId) {
        return articleService.removeAuthor(articleId, authorId);
    }

    // ─── AI Annotation ───────────────────────────────────────────────────────
    @PostMapping("/{articleId}/annotate")
    public Map<String, Object> annotate(@PathVariable Long articleId,
                                        @RequestParam Long promptId,
                                        @RequestParam(defaultValue = "false") boolean useFullText) {
        return articleService.annotate(articleId, promptId, useFullText);
    }

    @PostMapping("/{articleId}/analyze-gemma")
    public ResponseEntity<String> analyzeWithGemma(
            @PathVariable String articleId,
            @RequestBody Map<String, String> requestPayload) {

        String prompt = requestPayload.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"error\": \"The 'prompt' property is required.\"}");
        }

        // Invoke the client utility method connected to NVIDIA NIM API
        String rawGemmaResponse = gemmaService.callGemma(prompt);

        // Clean out raw markdown wrapper structures before passing back down
        String cleanJson = rawGemmaResponse.trim();
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.replaceAll("^```(?:json)?\\s*", "");
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3).trim();
        }

        // Write the cleaned JSON directly as a raw String response body with JSON Content-Type headers
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(cleanJson);
    }

    @PutMapping("/{articleId}/review")
    public ResponseEntity<Review> saveReview(
            @PathVariable Long articleId,
            @RequestBody ReviewSubmissionDto submissionDto) {

        Long mockReviewerId = 1L; // Replace with Principal / security session configurations later
        Review updatedReview = reviewService.saveOrUpdateReview(articleId, mockReviewerId, submissionDto);
        return ResponseEntity.ok(updatedReview);
    }

    // Endpoint for hydration and structural lookup during React setup mounting cycles
    @GetMapping("/{articleId}/review-data")
    public ResponseEntity<Review> getReviewData(@PathVariable Long articleId) {
        Long mockReviewerId = 1L;
        return reviewService.getReviewByArticleAndReviewer(articleId, mockReviewerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build()); // Returns 204 if no previous review exists
    }
}