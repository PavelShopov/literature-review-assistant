package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.config.GeminiConfig;
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
public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

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

    public AskResult ask(String abstractText, String promptText) {
        String fullPrompt = promptText +
                "\n\nArticle abstract:\n" + abstractText +
                "\n\nRespond in JSON format: {\"matches\": true/false, \"reason\": \"...\"}";

        String raw = callGemini(fullPrompt);
        return new AskResult(raw);
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

    public record AskResult(String rawJson) {
    }
}
