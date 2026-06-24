package mk.ukim.finki.literaturereviewassistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GemmaConfig {

    @Value("${nvidia.nim.api.key}")
    private String apiKey;

    @Value("${nvidia.nim.model:google/diffusiongemma-26b-a4b-it}")
    private String modelName;

//    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelName() {
        return modelName;
    }
}