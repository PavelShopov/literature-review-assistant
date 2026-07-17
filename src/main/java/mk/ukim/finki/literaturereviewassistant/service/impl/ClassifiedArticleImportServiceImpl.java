package mk.ukim.finki.literaturereviewassistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.ArticleClassification;
import mk.ukim.finki.literaturereviewassistant.model.ArticleStatus;
import mk.ukim.finki.literaturereviewassistant.model.ArticleSurvey;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyDimension;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyValue;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleClassificationRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleSurveyRepository;
import mk.ukim.finki.literaturereviewassistant.repository.SurveyRepository;
import mk.ukim.finki.literaturereviewassistant.repository.TaxonomyDimensionRepository;
import mk.ukim.finki.literaturereviewassistant.repository.TaxonomyValueRepository;
import mk.ukim.finki.literaturereviewassistant.service.ClassifiedArticleImportService;
import mk.ukim.finki.literaturereviewassistant.service.TaxonomyTextNormalizer;
import mk.ukim.finki.literaturereviewassistant.web.dto.ClassifiedArticleImportSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClassifiedArticleImportServiceImpl implements ClassifiedArticleImportService {

    private static final Logger log = LoggerFactory.getLogger(ClassifiedArticleImportServiceImpl.class);

    private final SurveyRepository surveyRepository;
    private final ArticleRepository articleRepository;
    private final ArticleSurveyRepository articleSurveyRepository;
    private final TaxonomyDimensionRepository taxonomyDimensionRepository;
    private final TaxonomyValueRepository taxonomyValueRepository;
    private final ArticleClassificationRepository articleClassificationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClassifiedArticleImportServiceImpl(
            SurveyRepository surveyRepository,
            ArticleRepository articleRepository,
            ArticleSurveyRepository articleSurveyRepository,
            TaxonomyDimensionRepository taxonomyDimensionRepository,
            TaxonomyValueRepository taxonomyValueRepository,
            ArticleClassificationRepository articleClassificationRepository) {
        this.surveyRepository = surveyRepository;
        this.articleRepository = articleRepository;
        this.articleSurveyRepository = articleSurveyRepository;
        this.taxonomyDimensionRepository = taxonomyDimensionRepository;
        this.taxonomyValueRepository = taxonomyValueRepository;
        this.articleClassificationRepository = articleClassificationRepository;
    }

    @Override
    @Transactional
    public ClassifiedArticleImportSummary importClassifiedArticles(String surveyId, MultipartFile file) {
        Survey survey = surveyRepository.findByExternalId(surveyId)
                .orElseThrow(() -> new EntityNotFoundException("Survey not found with external id: " + surveyId));
        JsonNode root = readRootArray(file);
        if (root.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Classified articles JSON array must not be empty");
        }
        validateRootSchema(root);

        log.info("Importing classified articles file '{}' into survey '{}' ({} articles)",
                safeFileName(file.getOriginalFilename()), surveyId, root.size());

        ClassifiedArticleImportSummary summary = new ClassifiedArticleImportSummary();
        int index = 0;
        for (JsonNode articleNode : root) {
            String path = "$[" + index + "]";
            index++;
            importArticleNode(survey, articleNode, path, summary);
        }
        summary.finalizeWarnings();

        log.info("Classified article import completed for survey '{}': articles created={}, updated={}, skipped={}; classifications created={}, updated={}, skipped={}; warnings={}, errors={}",
                surveyId,
                summary.getArticlesCreated(),
                summary.getArticlesUpdated(),
                summary.getArticlesSkipped(),
                summary.getClassificationsCreated(),
                summary.getClassificationsUpdated(),
                summary.getClassificationsSkipped(),
                summary.getWarnings().size(),
                summary.getErrors().size());
        return summary;
    }

    private JsonNode readRootArray(MultipartFile file) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Classified articles JSON file is required");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Classified articles JSON file is empty");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read classified articles JSON file", e);
        }

        if (bytes.length == 0 || new String(bytes, StandardCharsets.UTF_8).trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Classified articles JSON file is empty");
        }

        try {
            JsonNode root = objectMapper.readTree(bytes);
            if (!root.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Classified articles JSON root must be an array");
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded classified articles file is not valid JSON", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to parse classified articles JSON file", e);
        }
    }

    private void validateRootSchema(JsonNode root) {
        int articleIndex = 0;
        for (JsonNode articleNode : root) {
            String articlePath = "$[" + articleIndex + "]";
            articleIndex++;
            if (!articleNode.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, articlePath + " must be an object");
            }

            JsonNode classificationRoot = articleNode.get("classification");
            if (classificationRoot == null || !classificationRoot.isObject()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        articlePath + ".classification object is required"
                );
            }

            JsonNode classifications = classificationRoot.get("classifications");
            if (classifications == null || !classifications.isArray()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        articlePath + ".classification.classifications array is required"
                );
            }
        }
    }

    private void importArticleNode(Survey survey, JsonNode articleNode, String path, ClassifiedArticleImportSummary summary) {
        if (!articleNode.isObject()) {
            summary.getErrors().add(path + " must be an object; skipped.");
            summary.incrementArticlesSkipped();
            return;
        }

        String doi = normalizeDoi(optionalText(articleNode, "doi"));
        String title = optionalText(articleNode, "title");
        Integer year = optionalYear(articleNode.get("year"), path + ".year", summary);

        Optional<Article> existing = findExistingArticle(doi, title, year);
        if (existing.isEmpty() && !hasText(doi) && !hasText(title)) {
            summary.getErrors().add(path + " requires at least a DOI or title to create an article; skipped.");
            summary.incrementArticlesSkipped();
            return;
        }

        boolean created = existing.isEmpty();
        Article article = existing.orElseGet(Article::new);
        boolean articleChanged = applyArticleMetadata(article, doi, title, year);
        if (created) {
            article.setExternalId("article-" + UUID.randomUUID());
            article.setStatus("PENDING");
        }

        Article savedArticle = articleRepository.save(article);
        if (created) {
            summary.incrementArticlesCreated();
        } else if (articleChanged) {
            summary.incrementArticlesUpdated();
        } else {
            summary.incrementArticlesSkipped();
        }

        linkArticleToSurvey(savedArticle, survey, summary);
        importClassifications(savedArticle, articleNode, path, summary);
    }

    private Optional<Article> findExistingArticle(String doi, String title, Integer year) {
        if (hasText(doi)) {
            Optional<Article> byDoi = articleRepository.findByDoi(doi);
            if (byDoi.isPresent()) {
                return byDoi;
            }
        }
        if (hasText(title) && year != null) {
            return articleRepository.findFirstByTitleIgnoreCaseAndPublicationYear(title, year);
        }
        return Optional.empty();
    }

    private boolean applyArticleMetadata(Article article, String doi, String title, Integer year) {
        boolean changed = false;

        if (hasText(doi) && !Objects.equals(article.getDoi(), doi)) {
            if (!hasText(article.getDoi())) {
                article.setDoi(doi);
                changed = true;
            }
        }
        if (hasText(title) && !Objects.equals(article.getTitle(), title)) {
            if (!hasText(article.getTitle())) {
                article.setTitle(title);
                changed = true;
            }
        }
        if (year != null && !Objects.equals(article.getPublicationYear(), year)) {
            if (article.getPublicationYear() == null) {
                article.setPublicationYear(year);
                changed = true;
            }
        }
        return changed;
    }

    private void linkArticleToSurvey(Article article, Survey survey, ClassifiedArticleImportSummary summary) {
        if (articleSurveyRepository.findBySurveyAndArticle(survey, article).isPresent()) {
            summary.incrementSurveyLinksSkipped();
            return;
        }

        ArticleSurvey link = new ArticleSurvey();
        link.setArticle(article);
        link.setSurvey(survey);
        link.setStatus(ArticleStatus.PENDING);
        articleSurveyRepository.save(link);
        summary.incrementSurveyLinksCreated();
    }

    private void importClassifications(Article article, JsonNode articleNode, String articlePath, ClassifiedArticleImportSummary summary) {
        JsonNode classificationRoot = articleNode.get("classification");
        if (classificationRoot == null || !classificationRoot.isObject()) {
            summary.getErrors().add(articlePath + ".classification object is required; classifications skipped.");
            return;
        }

        JsonNode classifications = classificationRoot.get("classifications");
        if (classifications == null || !classifications.isArray()) {
            summary.getErrors().add(articlePath + ".classification.classifications array is required; classifications skipped.");
            return;
        }

        int index = 0;
        for (JsonNode classificationNode : classifications) {
            String path = articlePath + ".classification.classifications[" + index + "]";
            index++;
            importClassificationNode(article, classificationNode, path, summary);
        }
    }

    private void importClassificationNode(Article article, JsonNode classificationNode, String path, ClassifiedArticleImportSummary summary) {
        if (!classificationNode.isObject()) {
            summary.addWarning(path + " must be an object; skipped.");
            summary.incrementClassificationsSkipped();
            return;
        }

        String dimensionName = optionalText(classificationNode, "dimension");
        if (!hasText(dimensionName)) {
            summary.addWarning(path + ".dimension is required; skipped.");
            summary.incrementClassificationsSkipped();
            return;
        }

        Optional<TaxonomyDimension> dimension = taxonomyDimensionRepository
                .findByNormalizedName(TaxonomyTextNormalizer.normalize(dimensionName));
        if (dimension.isEmpty()) {
            summary.incrementWarningCount("classifications were skipped because the taxonomy dimension was unknown");
            summary.incrementClassificationsSkipped();
            return;
        }

        JsonNode values = classificationNode.get("values");
        if (values == null || values.isNull() || (values.isArray() && values.isEmpty())) {
            summary.incrementWarningCount("classifications were skipped because they had no taxonomy values");
            summary.incrementClassificationsSkipped();
            return;
        }
        if (!values.isArray()) {
            summary.addWarning(path + ".values must be an array or null; skipped.");
            summary.incrementClassificationsSkipped();
            return;
        }

        String confidence = optionalText(classificationNode, "confidence");
        String proof = optionalText(classificationNode, "proof");

        int valueIndex = 0;
        for (JsonNode valueNode : values) {
            valueIndex++;
            if (!valueNode.isTextual() || valueNode.asText().trim().isBlank()) {
                summary.incrementWarningCount("classification values were skipped because they were blank");
                summary.incrementClassificationsSkipped();
                continue;
            }

            String valueText = valueNode.asText().trim();
            Optional<TaxonomyValue> taxonomyValue = taxonomyValueRepository
                    .findByDimensionAndNormalizedValue(dimension.get(), TaxonomyTextNormalizer.normalize(valueText));
            if (taxonomyValue.isEmpty()) {
                summary.incrementWarningCount("classification values were skipped because the taxonomy value was unknown");
                summary.incrementClassificationsSkipped();
                continue;
            }

            saveClassification(article, dimension.get(), taxonomyValue.get(), confidence, proof, summary);
        }
    }

    private void saveClassification(
            Article article,
            TaxonomyDimension dimension,
            TaxonomyValue value,
            String confidence,
            String proof,
            ClassifiedArticleImportSummary summary) {
        ArticleClassification classification = articleClassificationRepository
                .findByArticleAndTaxonomyDimensionAndTaxonomyValue(article, dimension, value)
                .orElseGet(ArticleClassification::new);
        boolean created = classification.getArticleClassificationId() == null;

        classification.setArticle(article);
        classification.setTaxonomyDimension(dimension);
        classification.setTaxonomyValue(value);

        boolean changed = false;
        if (!Objects.equals(classification.getConfidence(), confidence)) {
            classification.setConfidence(confidence);
            changed = true;
        }
        if (!Objects.equals(classification.getProof(), proof)) {
            classification.setProof(proof);
            changed = true;
        }

        articleClassificationRepository.save(classification);
        if (created) {
            summary.incrementClassificationsCreated();
        } else if (changed) {
            summary.incrementClassificationsUpdated();
        } else {
            summary.incrementClassificationsSkipped();
        }
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            return value.asText(null);
        }
        String text = value.asText().trim();
        return text.isBlank() ? null : text;
    }

    private Integer optionalYear(JsonNode node, String path, ClassifiedArticleImportSummary summary) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.isTextual() ? node.asText().trim() : node.asText();
        if (!hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            summary.addWarning(path + " must be a valid year; article year was ignored.");
            return null;
        }
    }

    private String normalizeDoi(String input) {
        if (!hasText(input)) {
            return null;
        }
        String doi = input.trim()
                .replaceFirst("(?i)^doi:\\s*", "")
                .replaceFirst("(?i)^https?://(dx\\.)?doi\\.org/", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return doi.isBlank() ? null : doi;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded classified articles";
        }
        return originalFilename.replaceAll("[\\r\\n\\t]", " ").trim();
    }
}
