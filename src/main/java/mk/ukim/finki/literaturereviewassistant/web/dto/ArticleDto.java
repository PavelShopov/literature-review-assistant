package mk.ukim.finki.literaturereviewassistant.web.dto;

import lombok.Data;
import java.util.List;

@Data
public class ArticleDto {
    private String id;
    private String title;
    private List<String> authors;
    private String journal;
    private Integer year;
    private String doi;
    private String status;
    // Map to articleAbstract
    private String abstractText; 
    private String inclusionSummary;
    private AddedByDto addedBy;

    @Data
    public static class AddedByDto {
        private String id;
        private String name;
        private String role;
    }
}
