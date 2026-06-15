package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
* Meant to act as a middle so we can control the status for each article in each survey
* Can be changed to store more info if needed
*/

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

    @ManyToOne
    @JoinColumn(name = "Suravey_id")
    private Survey survey;

    @ManyToOne
    @JoinColumn(name = "Article_id")
    private Article article;

}
