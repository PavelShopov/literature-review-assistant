package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import java.time.Instant;

/*
* The reviewer class each reviewer is connected to a user
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Reviewer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewerId;

    private String externalId;
    private String name;
    private String email;
    private String role;
    private Instant addedDate;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "AppUser_id")
    private AppUser AppUser;

    @ManyToMany(cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "reviewer_survey", // Name of the hidden join table in the DB
            joinColumns = @JoinColumn(name = "reviewer_id"), // FK pointing to Reviewer
            inverseJoinColumns = @JoinColumn(name = "survey_id")  // FK pointing to Survey
    )
    private List<Survey> surveys = new ArrayList<>();

    @OneToMany(mappedBy = "Reviewer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;
}
