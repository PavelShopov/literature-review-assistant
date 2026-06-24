package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.config.OllamaConfig;
import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Prompt;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    private final RestTemplate restTemplate;
    private final OllamaConfig ollamaConfig;

    public OllamaService(RestTemplate restTemplate, OllamaConfig ollamaConfig) {
        this.restTemplate = restTemplate;
        this.ollamaConfig = ollamaConfig;
    }

    public String annotate(String abstractText, String promptText) {
        String fullPrompt = promptText + "\n\nArticle abstract:\n" + abstractText;
        return callOllama(fullPrompt);
    }

    public String chat(String prompt) {
        return callOllama(prompt);
    }

    public AnswerResult annotateArticle(Article article, Prompt prompt) {
        String fullPrompt = prompt.getPromptText() +
                "\n\nArticle title: " + article.getTitle() +
                "\n\nArticle abstract:\n" + article.getArticleAbstract() +
                "\n\nRespond ONLY in JSON format: {\"include\": true/false, \"explanation\": \"...\"}";

        String raw = callOllama(fullPrompt);
        String cleaned = raw.replace("```json", "").replace("```", "").trim();
        boolean include = cleaned.contains("\"include\": true") || cleaned.contains("\"include\":true");
        return new AnswerResult(include, cleaned, cleaned);
    }

    public AskResult ask(String abstractText, String promptText) {
        String fullPrompt = promptText +
                "\n\nArticle abstract:\n" + abstractText +
                "\n\nRespond in JSON format: {\"matches\": true/false, \"reason\": \"...\"}";

        return new AskResult(callOllama(fullPrompt));
    }

    private String callOllama(String prompt) {
        String apiUrl = ollamaConfig.getApiUrl();
        String model = ollamaConfig.getModel();
        if (apiUrl == null || apiUrl.isBlank()) {
            return "{\"answer\": false, \"explanation\": \"Ollama API URL is not configured.\"}";
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Object result = response.getBody().get("response");
            return result != null ? result.toString() : "{}";
        }

        throw new RuntimeException("Ollama API call failed with status: " + response.getStatusCode());
    }

    public record AskResult(String rawJson) {
    }

    public record AnswerResult(boolean include, String explanation, String rawJson) {
    }
}
