package mk.ukim.finki.literaturereviewassistant.service.DataService;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ArticleMetadata {
    private String title;
    private String doi;
    private String url;
    private String journal;
    private Integer year;
    private String abstractText;
    private List<String> authorNames = new ArrayList<>();
    private byte[] pdfBytes;
}
