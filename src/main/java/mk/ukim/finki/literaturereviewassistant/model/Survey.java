package mk.ukim.finki.literaturereviewassistant.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

import static lombok.ToString.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surveyId;

    private String title;

    @ElementCollection
    private List<String> keywords;


    @ManyToMany(mappedBy = "surveys")
    @Exclude
    @EqualsAndHashCode.Exclude
    private List<Article> articles;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Prompts> prompts;
}