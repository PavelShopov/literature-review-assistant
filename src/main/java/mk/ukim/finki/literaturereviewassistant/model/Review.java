package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
* The review that is left from each Reviewer onto each Article
* if needed it can be changed to be per article per survey
*/

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(columnDefinition = "TEXT")
    private String jsonResponse;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne
    @JoinColumn(name = "Reviewer_id")
    private Reviewer reviewer;
}
