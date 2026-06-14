package mk.ukim.finki.literaturereviewassistant.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/surveys")
@CrossOrigin(origins = "http://localhost:5173")
public class SurveyController {

    @GetMapping
    public List<Map<String, Object>> getAllSurveys() {

        return List.of(
                Map.of(
                        "id", "survey-001",
                        "name", "Impact of AI on Healthcare Outcomes",
                        "description", "A systematic literature review examining AI in healthcare.",
                        "createdDate", "2024-03-15",
                        "status", "In Progress",
                        "totalArticles", 6
                ),
                Map.of(
                        "id", "survey-002",
                        "name", "Machine Learning in Education",
                        "description", "Research about ML applications in education.",
                        "createdDate", "2024-02-10",
                        "status", "Completed",
                        "totalArticles", 12
                )
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getSurveyById(@PathVariable String id) {

        return Map.of(
                "id", id,
                "name", "Impact of AI on Healthcare Outcomes",
                "description", "A systematic literature review examining AI in healthcare.",
                "researchQuestion", "How effective are AI technologies in improving patient outcomes?",
                "createdDate", "2024-03-15",
                "status", "In Progress",
                "totalArticles", 6,
                "screened", 4,
                "pending", 2
        );
    }

    @GetMapping("/articles/{articleId}")
    public Map<String, Object> getArticleById(@PathVariable String articleId) {

        return Map.of(
                "id", articleId,
                "title", "Deep Learning Applications in Medical Image Analysis",
                "authors", List.of("Eva Knezevik", "Johnson, A."),
                "journal", "Journal of Medical Imaging",
                "year", 2024,
                "doi", "10.1000/jmi.2024.001",
                "status", "INCLUDED",
                "abstract", "This systematic review examines AI applications in healthcare."
        );
    }
}