package mk.ukim.finki.literaturereviewassistant.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/surveys")
@CrossOrigin(origins = "http://localhost:5173")
public class TestController {

    @GetMapping
    public List<Map<String, Object>> getAllSurveys() {
        return List.of(
                Map.of(
                        "id", 1,
                        "title", "AI in Healthcare Literature Review",
                        "description", "Review about AI applications in healthcare.",
                        "articlesCount", 12
                ),
                Map.of(
                        "id", 2,
                        "title", "Machine Learning in Education",
                        "description", "Survey about ML tools for student learning.",
                        "articlesCount", 8
                )
        );
    }
}