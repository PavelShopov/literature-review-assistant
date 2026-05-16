package mk.ukim.finki.literaturereviewassistant.model;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleSurvey {
    @Id
    private Long id;

    @ManyToOne
    private Article article;

    @ManyToOne
    private Survey survey;
}
