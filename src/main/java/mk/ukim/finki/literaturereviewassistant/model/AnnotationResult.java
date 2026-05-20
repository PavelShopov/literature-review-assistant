package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnnotationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long annotationResultId;

    @Column(columnDefinition = "TEXT")
    private String jsonResponse; // Holds structural AI evaluation metadata (true/false, reason)

    private Boolean isManuallyEdited = false;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne
    @JoinColumn(name = "prompt_id")
    private Prompts prompt;

    @ManyToOne
    @JoinColumn(name = "survey_id")
    private Survey survey;
}