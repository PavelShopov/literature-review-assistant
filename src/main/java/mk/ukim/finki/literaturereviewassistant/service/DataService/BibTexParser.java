package mk.ukim.finki.literaturereviewassistant.service.DataService;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class BibTexParser {
    public List<BibEntry> parse(MultipartFile bibFile) {
        return null;
    }
}
