package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.config.GeminiConfig; // Adjust or use GemmaConfig
import mk.ukim.finki.literaturereviewassistant.config.GemmaConfig;
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
public class GemmaService {

    private static final String NIM_URL = "https://integrate.api.nvidia.com/v1/chat/completions";

    private final RestTemplate restTemplate;
    private final GemmaConfig gemmaConfig;

    public GemmaService(RestTemplate restTemplate, GemmaConfig gemmaConfig) {
        this.restTemplate = restTemplate;
        this.gemmaConfig = gemmaConfig;
    }

    @SuppressWarnings("rawtypes")
    public String callGemma(String prompt) {
        String apiKey = gemmaConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("NVIDIA NIM API key is not configured.");
        }

        // OpenAI-Compatible Payload Format for NIM
        Map<String, Object> requestBody = Map.of(
                "model", gemmaConfig.getModelName(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 1.0,  // Recommended default for Gemma 4
                "top_p", 0.95,
                "max_tokens", 2048
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey); // Sets "Authorization: Bearer <key>"

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(NIM_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                try {
                    // Extract data out of OpenAI payload pattern: choices -> message -> content
                    List<?> choices = (List<?>) response.getBody().get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                        Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                        if (message != null) {
                            return (String) message.get("content");
                        }
                    }
                    throw new RuntimeException("Empty choices structure received from NVIDIA NIM API");
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse Gemma 4 response payload structure", e);
                }
            }
            throw new RuntimeException("NVIDIA NIM returned unexpected status: " + response.getStatusCode());

        } catch (HttpStatusCodeException e) {
            String errorResponse = e.getResponseBodyAsString();
            System.err.println("--- NVIDIA NIM API Error Details ---");
            System.err.println("Status Code: " + e.getStatusCode());
            System.err.println("Response Body: " + errorResponse);
            System.err.println("------------------------------------");
            throw new RuntimeException("NVIDIA NIM network call failed: " + e.getMessage() + " -> " + errorResponse, e);
        } catch (Exception e) {
            throw new RuntimeException("Generic failure during communication with NVIDIA NIM", e);
        }
    }
}