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
    @Column(nullable = false, unique = true)
    private String externalId;

    private String surveyExternalId;

    private String url;
    private String title;
    private String doi;
    private String journal;
    private Integer publicationYear;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String inclusionSummary;

    private String addedById;
    private String addedByName;
    private String addedByRole;

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
    private List<Author> authors = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ArticleSurvey> surveyLinks = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AnnotationResult> annotationResults = new ArrayList<>();
}
