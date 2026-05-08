package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Prompts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long promptId;

    @Column(columnDefinition = "TEXT")
    private String promptText;

    @Column(columnDefinition = "TEXT")
    private String promptJsonOutput; // Used to hold expected target schema rules

    @ManyToOne
    @JoinColumn(name = "survey_id")
    private Survey survey;
}