package mk.ukim.finki.literaturereviewassistant.service.DataService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractorService {
    public ArticleMetadata extractFromUrl(String url) {
        ArticleMetadata metadata = new ArticleMetadata();
        try (InputStream in = new URL(url).openStream()) {
             byte[] bytes = in.readAllBytes();
             metadata = extractFromBytes(bytes, url);
             metadata.setUrl(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadata;
    }

    public ArticleMetadata extractFromBytes(byte[] bytes) {
        return extractFromBytes(bytes, null);
    }

    public ArticleMetadata extractFromBytes(byte[] bytes, String fallbackName) {
        ArticleMetadata metadata = new ArticleMetadata();
        metadata.setPdfBytes(bytes);
        
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            metadata.setAbstractText(extractAbstract(text));

            PDDocumentInformation info = document.getDocumentInformation();
            if (info != null) {
                if (info.getTitle() != null) metadata.setTitle(info.getTitle());
                if (info.getAuthor() != null) {
                    metadata.setAuthorNames(Arrays.asList(info.getAuthor().split(",")));
                }
            }

            if ((metadata.getTitle() == null || metadata.getTitle().isBlank()) && text != null) {
                metadata.setTitle(extractTitle(text, fallbackName));
            }
            if (metadata.getAuthorNames() == null || metadata.getAuthorNames().isEmpty()) {
                metadata.setAuthorNames(extractAuthors(text));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadata;
    }

    private String extractTitle(String text, String fallbackName) {
        if (text != null) {
            String best = null;
            for (String line : text.split("\\R")) {
                String cleaned = line.replaceAll("\\s+", " ").trim();
                if (cleaned.isBlank()) {
                    continue;
                }
                if (looksLikeSectionHeader(cleaned) || looksLikeAuthorLine(cleaned)) {
                    continue;
                }
                if (best == null || cleaned.length() > best.length()) {
                    best = cleaned;
                }
            }

            if (best != null && best.length() > 5) {
                return best.length() > 180 ? best.substring(0, 180) : best;
            }
        }

        if (fallbackName != null && !fallbackName.isBlank()) {
            String cleaned = fallbackName.replaceAll("\\.[Pp][Dd][Ff]$", "").replace('_', ' ').replace('-', ' ').trim();
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }

        return "Untitled PDF Article";
    }

    private List<String> extractAuthors(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> authors = new ArrayList<>();
        String[] lines = text.split("\\R");
        for (int i = 0; i < Math.min(lines.length, 8); i++) {
            String cleaned = lines[i].replaceAll("\\s+", " ").trim();
            if (cleaned.isBlank() || looksLikeSectionHeader(cleaned)) {
                continue;
            }
            if (looksLikeAuthorLine(cleaned)) {
                String[] parts = cleaned.split("\\s+(?:and|&|,|; )\\s*");
                for (String part : parts) {
                    String author = part.trim();
                    if (!author.isBlank() && author.length() > 2) {
                        authors.add(author);
                    }
                }
                if (!authors.isEmpty()) {
                    return authors;
                }
            }
        }

        return List.of();
    }

    private boolean looksLikeSectionHeader(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("abstract")
                || lower.startsWith("keywords")
                || lower.startsWith("introduction")
                || lower.startsWith("references")
                || lower.matches("^\\d+\\s+.*");
    }

    private boolean looksLikeAuthorLine(String value) {
        return value.length() <= 120
                && !value.contains("@")
                && (value.contains(",") || value.contains(" and ") || value.contains("&"))
                && !value.matches(".*\\b(university|institute|department|school|college|laboratory)\\b.*");
    }

    private String extractAbstract(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = text.replace('\r', '\n');
        int abstractIndex = normalized.toLowerCase().indexOf("abstract");
        if (abstractIndex >= 0) {
            String snippet = normalized.substring(abstractIndex + "abstract".length());
            snippet = snippet.replaceFirst("^[\\s:.-]+", "");
            int endIndex = findSectionBreak(snippet);
            if (endIndex > 0) {
                snippet = snippet.substring(0, endIndex);
            }
            return snippet.replaceAll("\\s+", " ").trim();
        }

        return normalized.substring(0, Math.min(normalized.length(), 2000)).replaceAll("\\s+", " ").trim();
    }

    private int findSectionBreak(String text) {
        String lower = text.toLowerCase();
        int[] markers = {
                lower.indexOf("\nintroduction"),
                lower.indexOf("\n1 introduction"),
                lower.indexOf("\nkeywords"),
                lower.indexOf("\nreferences")
        };

        int end = -1;
        for (int marker : markers) {
            if (marker > 0 && (end == -1 || marker < end)) {
                end = marker;
            }
        }
        return end;
    }
}
