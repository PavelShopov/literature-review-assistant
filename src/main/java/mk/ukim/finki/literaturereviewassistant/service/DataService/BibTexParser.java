package mk.ukim.finki.literaturereviewassistant.service.DataService;

import org.jbibtex.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class BibTexParser {
    public List<BibEntry> parse(MultipartFile bibFile) {
        List<BibEntry> entries = new ArrayList<>();
        try (Reader reader = new InputStreamReader(bibFile.getInputStream(), StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return entries;
    }

    public List<BibEntry> parse(String bibtexText) {
        try (Reader reader = new StringReader(bibtexText)) {
            return parse(reader);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed BibTeX input", e);
        }
    }

    private List<BibEntry> parse(Reader reader) throws Exception {
        List<BibEntry> entries = new ArrayList<>();
        org.jbibtex.BibTeXParser bibtexParser = new org.jbibtex.BibTeXParser();
        BibTeXDatabase database = bibtexParser.parse(reader);

        for (BibTeXEntry entry : database.getEntries().values()) {
            entries.add(toBibEntry(entry));
        }
        return entries;
    }

    private BibEntry toBibEntry(BibTeXEntry entry) {
        BibEntry bibEntry = new BibEntry();

        Value doiValue = entry.getField(BibTeXEntry.KEY_DOI);
        if (doiValue != null) bibEntry.setDoi(doiValue.toUserString().trim());

        Value titleValue = entry.getField(BibTeXEntry.KEY_TITLE);
        if (titleValue != null) bibEntry.setTitle(cleanText(titleValue.toUserString()));

        Value urlValue = entry.getField(BibTeXEntry.KEY_URL);
        if (urlValue != null) bibEntry.setUrl(urlValue.toUserString().trim());

        Value abstractValue = entry.getField(new Key("abstract"));
        if (abstractValue != null) bibEntry.setAbstractText(cleanText(abstractValue.toUserString()));

        Value journalValue = entry.getField(BibTeXEntry.KEY_JOURNAL);
        if (journalValue != null) {
            bibEntry.setJournal(cleanText(journalValue.toUserString()));
        }

        Value yearValue = entry.getField(BibTeXEntry.KEY_YEAR);
        if (yearValue != null) {
            bibEntry.setYear(yearValue.toUserString().trim());
        }

        Value authorValue = entry.getField(BibTeXEntry.KEY_AUTHOR);
        if (authorValue != null) {
            String[] authors = authorValue.toUserString().split(" and ");
            bibEntry.setAuthorNames(Arrays.stream(authors).map(String::trim).filter(s -> !s.isBlank()).toList());
        }

        if (bibEntry.getTitle() == null || bibEntry.getTitle().isBlank()) {
            bibEntry.setTitle("Untitled BibTeX Article");
        }

        if (bibEntry.getJournal() == null || bibEntry.getJournal().isBlank()) {
            Value bookTitleValue = entry.getField(BibTeXEntry.KEY_BOOKTITLE);
            if (bookTitleValue != null) {
                bibEntry.setJournal(cleanText(bookTitleValue.toUserString()));
            }
        }

        return bibEntry;
    }

    private String cleanText(String value) {
        return value == null ? null : value.replaceAll("\\s+", " ").trim();
    }
}
