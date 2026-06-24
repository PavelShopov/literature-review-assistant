package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.config.GeminiConfig;
import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Prompt;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private final RestTemplate restTemplate;
    private final GeminiConfig geminiConfig;

    public GeminiService(RestTemplate restTemplate, GeminiConfig geminiConfig) {
        this.restTemplate = restTemplate;
        this.geminiConfig = geminiConfig;
    }

    public String annotate(String abstractText, String promptText) {
        String fullPrompt = promptText + "\n\nArticle abstract:\n" + abstractText;
        return callGemini(fullPrompt);
    }

    public AnswerResult annotateArticle(Article article, Prompt prompt) {
        String fullPrompt = prompt.getPromptText() +
                "\n\nArticle title: " + article.getTitle() +
                "\n\nArticle abstract:\n" + article.getArticleAbstract() +
                "\n\nRespond ONLY in JSON format: {\"include\": true/false, \"explanation\": \"...\"}";

        String raw = callGemini(fullPrompt);
        String cleaned = raw.replace("```json", "").replace("```", "").trim();
        boolean include = cleaned.contains("\"include\": true") || cleaned.contains("\"include\":true");
        return new AnswerResult(include, cleaned, cleaned);
    }

    public AskResult ask(String abstractText, String promptText) {
        String fullPrompt = promptText +
                "\n\nArticle abstract:\n" + abstractText +
                "\n\nRespond in JSON format: {\"matches\": true/false, \"reason\": \"...\"}";

        return new AskResult(callGemini(fullPrompt));
    }

    @SuppressWarnings("rawtypes")
    public String callGemini(String prompt) {
        String apiKey = geminiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "{\"include\": false, \"explanation\": \"Gemini API key is not configured.\"}";
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", prompt))
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // Using Map.class requires robust down-casting to deep structural Maps
            ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_URL + apiKey, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                try {
                    // FIX 2: Safe type casting extraction from Gemini standard response structure
                    List<?> candidates = (List<?>) response.getBody().get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                        Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                        if (content != null) {
                            List<?> parts = (List<?>) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                                return (String) firstPart.get("text");
                            }
                        }
                    }
                    throw new RuntimeException("Empty or invalid candidates structure from Gemini API");
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse Gemini response payload structure", e);
                }
            }
            throw new RuntimeException("Gemini API call returned unexpected status: " + response.getStatusCode());

        } catch (HttpStatusCodeException e) {
            String errorResponse = e.getResponseBodyAsString();
            System.err.println("--- Gemini API Error Details ---");
            System.err.println("Status Code: " + e.getStatusCode());
            System.err.println("Response Body: " + errorResponse);
            System.err.println("--------------------------------");

            throw new RuntimeException("Gemini API network call failed: " + e.getMessage() + " -> " + errorResponse, e);
        } catch (Exception e) {
            throw new RuntimeException("Generic failure during Gemini communication", e);
        }
    }

    public record AskResult(String rawJson) {
    }

    public record AnswerResult(boolean include, String explanation, String rawJson) {
    }
}