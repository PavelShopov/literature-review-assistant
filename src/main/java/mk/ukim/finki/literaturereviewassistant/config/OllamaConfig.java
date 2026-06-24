package mk.ukim.finki.literaturereviewassistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String apiUrl;

    @Value("${ollama.model:qwen2.5:7b}")
    private String model;

    public String getApiUrl() {
        return apiUrl;
    }

    public String getModel() {
        return model;
    }
}
