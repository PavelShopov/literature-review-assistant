package mk.ukim.finki.literaturereviewassistant.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;
    private String title;
    private String type; // e.g., "PDF"
    private String filePath;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;
}

