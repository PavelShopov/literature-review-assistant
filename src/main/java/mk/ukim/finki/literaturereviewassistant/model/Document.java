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

    @Enumerated(EnumType.STRING)
    private DocumentType type;
    
    @Column(columnDefinition = "TEXT")
    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;
}

