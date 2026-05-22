package mk.ukim.finki.literaturereviewassistant.service.DataService;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class BibEntry {
    private String doi;
    private String title;
    private String url;
    private String abstractText;
    private List<String> authorNames = new ArrayList<>();

    public String getAbstract() {
        return abstractText;
    }
}
