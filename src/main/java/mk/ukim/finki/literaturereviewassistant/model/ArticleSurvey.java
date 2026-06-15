package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ArticleSurveyId;

    @Enumerated(EnumType.STRING)
    private ArticleStatus status;
}
