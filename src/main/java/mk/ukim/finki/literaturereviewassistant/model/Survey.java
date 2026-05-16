package mk.ukim.finki.literaturereviewassistant.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String Question;
    private String Title;
    private String Description;

    private List<String> keywords;

    @OneToMany(cascade = CascadeType.ALL)
    List<Prompt> prompts;
}