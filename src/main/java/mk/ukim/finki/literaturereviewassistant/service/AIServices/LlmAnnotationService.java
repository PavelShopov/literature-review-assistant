package mk.ukim.finki.literaturereviewassistant.service.AIServices;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Prompt;
import mk.ukim.finki.literaturereviewassistant.service.OllamaService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

@Service
public class LlmAnnotationService {
    private final OllamaService ollamaService;

    public LlmAnnotationService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public Map<String, Object> annotate(String context, Prompt prompt) {
        String raw = ollamaService.annotate(context, prompt.getPromptText());
        return normalizeAnnotateResult(raw);
    }

    public Map<String, Object> ask(String context, Prompt prompt) {
        OllamaService.AskResult result = ollamaService.ask(context, prompt.getPromptText());
        return normalizeAskResult(result.rawJson());
    }

    public void saveAnnotationResult(Article article, Prompt prompt, Map<String, Object> result) {
    }

    private Map<String, Object> normalizeAnnotateResult(String raw) {
        Map<String, Object> parsed = parseJsonMap(raw);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("include", readBoolean(parsed, "include", false));
        result.put("explanation", readString(parsed, "explanation", readString(parsed, "reason", raw)));
        result.put("rawJson", raw == null ? "" : raw);
        return result;
    }

    private Map<String, Object> normalizeAskResult(String raw) {
        Map<String, Object> parsed = parseJsonMap(raw);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", readBoolean(parsed, "answer", readBoolean(parsed, "matches", false)));
        result.put("explanation", readString(parsed, "explanation", readString(parsed, "reason", raw)));
        result.put("rawJson", raw == null ? "" : raw);
        return result;
    }

    private Map<String, Object> parseJsonMap(String raw) {
        String cleaned = raw == null ? "" : raw.replace("```json", "").replace("```", "").trim();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rawJson", cleaned);
        result.put("explanation", extractString(cleaned, "explanation", cleaned));
        result.put("reason", extractString(cleaned, "reason", cleaned));
        result.put("include", extractBoolean(cleaned, "include", false));
        result.put("answer", extractBoolean(cleaned, "answer", false));
        result.put("matches", extractBoolean(cleaned, "matches", false));
        return result;
    }

    private boolean extractBoolean(String text, String key, boolean fallback) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return fallback;
    }

    private String extractString(String text, String key, String fallback) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\"", "\"").trim();
        }
        return fallback == null ? "" : fallback;
    }

    private boolean readBoolean(Map<String, Object> parsed, String key, boolean fallback) {
        Object value = parsed.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return fallback;
    }

    private String readString(Map<String, Object> parsed, String key, String fallback) {
        Object value = parsed.get(key);
        if (value == null) {
            return fallback == null ? "" : fallback;
        }
        String text = value.toString().trim();
        return text.isBlank() ? (fallback == null ? "" : fallback) : text;
    }
}
