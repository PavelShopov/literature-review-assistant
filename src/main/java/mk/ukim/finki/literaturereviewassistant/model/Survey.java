package mk.ukim.finki.literaturereviewassistant.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

import static lombok.ToString.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surveyId;

    private String title;

    private String description;
    
    private java.time.LocalDate createdDate;
    
    private String status; // In Progress, Completed, Draft
    
    private String researchQuestion;

    @ElementCollection
    private List<String> keywords;


    @ManyToMany(mappedBy = "surveys")
    @Exclude
    @EqualsAndHashCode.Exclude
    private List<Article> articles;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Prompt> prompts;
}