package mk.ukim.finki.literaturereviewassistant.service.DataService;

import org.jbibtex.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class BibTexParser {
    public List<BibEntry> parse(MultipartFile bibFile) {
        List<BibEntry> entries = new ArrayList<>();
        try (Reader reader = new InputStreamReader(bibFile.getInputStream(), StandardCharsets.UTF_8)) {
            org.jbibtex.BibTeXParser bibtexParser = new org.jbibtex.BibTeXParser();
            BibTeXDatabase database = bibtexParser.parse(reader);

            for (BibTeXEntry entry : database.getEntries().values()) {
                BibEntry bibEntry = new BibEntry();

                Value doiValue = entry.getField(BibTeXEntry.KEY_DOI);
                if (doiValue != null) bibEntry.setDoi(doiValue.toUserString());

                Value titleValue = entry.getField(BibTeXEntry.KEY_TITLE);
                if (titleValue != null) bibEntry.setTitle(titleValue.toUserString());

                Value urlValue = entry.getField(BibTeXEntry.KEY_URL);
                if (urlValue != null) bibEntry.setUrl(urlValue.toUserString());

                Value abstractValue = entry.getField(new Key("abstract"));
                if (abstractValue != null) bibEntry.setAbstractText(abstractValue.toUserString());

                Value authorValue = entry.getField(BibTeXEntry.KEY_AUTHOR);
                if (authorValue != null) {
                    String[] authors = authorValue.toUserString().split(" and ");
                    bibEntry.setAuthorNames(Arrays.asList(authors));
                }

                entries.add(bibEntry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return entries;
    }
}
