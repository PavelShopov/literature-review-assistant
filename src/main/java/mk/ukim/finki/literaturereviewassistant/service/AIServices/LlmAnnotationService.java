package mk.ukim.finki.literaturereviewassistant.service.AIServices;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Prompt;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LlmAnnotationService {
    public Map<String, Object> annotate(String context, Prompt prompt) {
        return Map.of();
    }

    public void saveAnnotationResult(Article article, Prompt prompt, Map<String, Object> result) {
    }
}
