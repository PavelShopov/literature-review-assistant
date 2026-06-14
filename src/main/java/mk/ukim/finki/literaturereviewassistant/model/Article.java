package mk.ukim.finki.literaturereviewassistant.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleId;
    private String url;
    private String title;
    private String doi;

    @Column(columnDefinition = "TEXT")
    private String articleAbstract;

    @ManyToMany
    @JoinTable(
            name = "article_author",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Author> authors;

    @ManyToMany
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Survey> surveys;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Document> documents;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AnnotationResult> annotationResults;
}
