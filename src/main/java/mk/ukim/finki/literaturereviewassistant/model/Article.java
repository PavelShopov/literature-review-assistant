package mk.ukim.finki.literaturereviewassistant.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.Year;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String Abstract;
    private String DOI;
    private String Title;
    private String URL;

    private String DocumentName;
    private String LinkToSource;
    private Year Published;
    private String Journal;
}
