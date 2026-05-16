package mk.ukim.finki.literaturereviewassistant.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorArticle {
    @Id
    private Long id;

    @ManyToOne
    private Author author;

    @ManyToOne
    private Article article;
}
