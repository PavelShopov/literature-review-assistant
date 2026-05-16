package mk.ukim.finki.literaturereviewassistant.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.Year;
import java.util.List;

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
