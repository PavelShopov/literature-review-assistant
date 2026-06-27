package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter // Replaced @Data to prevent lazy initialization exceptions
@Setter // Replaced @Data to prevent lazy initialization exceptions
@AllArgsConstructor
@NoArgsConstructor
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surveyId;

    @Column(nullable = false, unique = true)
    private String externalId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String researchQuestion;

    private LocalDate createdDate;

    private String status;

    @ElementCollection
    @CollectionTable(name = "survey_keywords", joinColumns = @JoinColumn(name = "survey_id"))
    @Column(name = "keyword")
    private List<String> keywords = new ArrayList<>();

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ArticleSurvey> articleLinks = new ArrayList<>();

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Prompt> prompts = new ArrayList<>();

    // Added PERSIST so new reviewers can be saved together with a survey creation payload
    @ManyToMany(mappedBy = "surveys", cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @ToString.Exclude
    private List<Reviewer> reviewers = new ArrayList<>();


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "survey_selected_criteria",
            joinColumns = @JoinColumn(name = "survey_id")
    )
    @Column(name = "criterion_id")
    private Set<String> selectedCriteria = new HashSet<>();

    // AUTOMATION: Automatically sets the date and default status when created
    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDate.now();
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    // DEFENSIVE HELPER: Keeps the bidirectional Many-to-Many relationship fully synchronized
    public void addReviewer(Reviewer reviewer) {
        this.reviewers.add(reviewer);
        if (!reviewer.getSurveys().contains(this)) {
            reviewer.getSurveys().add(this);
        }
    }

    public void removeReviewer(Reviewer reviewer) {
        this.reviewers.remove(reviewer);
        reviewer.getSurveys().remove(this);
    }

    // SAFE IDENTIFIER: Overriding equals and hashCode based strictly on business key (externalId)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Survey)) return false;
        Survey survey = (Survey) o;
        return externalId != null && externalId.equals(survey.externalId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}