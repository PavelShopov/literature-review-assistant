package mk.ukim.finki.literaturereviewassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mk.ukim.finki.literaturereviewassistant.config.GeminiConfig;
import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Prompts;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private final RestTemplate restTemplate;
    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiService(RestTemplate restTemplate, GeminiConfig geminiConfig) {
        this.restTemplate = restTemplate;
        this.geminiConfig = geminiConfig;
    }

    /**
     * Праќа title + abstract на article и promptText до Gemini.
     * Враќа структуриран AnswerResult со include (true/false), explanation и rawJson.
     */
    public AnswerResult annotateArticle(Article article, Prompts prompt) {
        String fullPrompt = prompt.getPromptText() +
                "\n\nArticle title: " + article.getTitle() +
                "\n\nArticle abstract:\n" + article.getArticleAbstract() +
                "\n\nRespond ONLY in JSON format: {\"include\": true/false, \"explanation\": \"...\"}";

        String raw = callGemini(fullPrompt);

        try {
            String cleaned = raw.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode node = objectMapper.readTree(cleaned);
            boolean include = node.get("include").asBoolean();
            String explanation = node.has("explanation") ? node.get("explanation").asText() : "";
            return new AnswerResult(include, explanation, cleaned);
        } catch (Exception e) {
            return new AnswerResult(false, "Failed to parse Gemini response: " + e.getMessage(), raw);
        }
    }

    /**
     * Го повикува annotateArticle за секој article и ги враќа само оние со include = true.
     */
    public List<Article> getPositiveArticles(List<Article> articles, Prompts prompt) {
        return articles.stream()
                .filter(article -> annotateArticle(article, prompt).include())
                .collect(Collectors.toList());
    }

    private String callGemini(String prompt) {
        String url = GEMINI_URL + geminiConfig.getApiKey();

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            try {
                List candidates = (List) response.getBody().get("candidates");
                Map firstCandidate = (Map) candidates.get(0);
                Map content = (Map) firstCandidate.get("content");
                List parts = (List) content.get("parts");
                Map firstPart = (Map) parts.get(0);
                return (String) firstPart.get("text");
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Gemini response", e);
            }
        }

        throw new RuntimeException("Gemini API call failed with status: " + response.getStatusCode());
    }

    public record AnswerResult(boolean include, String explanation, String rawJson) {}
}