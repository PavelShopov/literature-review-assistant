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

    @ManyToMany(mappedBy = "articles")
    private List<Survey> surveys = new ArrayList<>();


// Source - https://stackoverflow.com/a/60799284
// Posted by twobiers, modified by community. See post 'Timeline' for change history
// Retrieved 2026-05-06, License - CC BY-SA 4.0

//    @ManyToMany(mappedBy = "survey")
//    var persons: MutableList<Survey> = mutableListOf()



    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Document> documents;
}
