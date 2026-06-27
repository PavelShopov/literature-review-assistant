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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfExtractorService {
    private static final Pattern DOI_PATTERN = Pattern.compile(
            "(?i)\\b10\\.\\d{4,9}/[-._;()/:A-Z0-9]+"
    );

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
            String firstPageText = extractFirstPageText(document);

            metadata.setAbstractText(extractAbstract(text));
            metadata.setDoi(extractDoi(text));

            PDDocumentInformation info = document.getDocumentInformation();
            if (info != null) {
                if (info.getTitle() != null && !isWeakTitle(info.getTitle(), fallbackName)) metadata.setTitle(info.getTitle());
                if (info.getAuthor() != null) {
                    metadata.setAuthorNames(parseAuthorField(info.getAuthor()));
                }
            }

            if ((metadata.getTitle() == null || metadata.getTitle().isBlank()) && text != null) {
                String titleSource = firstPageText != null && !firstPageText.isBlank() ? firstPageText : text;
                metadata.setTitle(extractTitle(titleSource, fallbackName));
            }
            if (metadata.getAuthorNames() == null || metadata.getAuthorNames().isEmpty()) {
                String authorSource = firstPageText != null && !firstPageText.isBlank() ? firstPageText : text;
                metadata.setAuthorNames(extractAuthors(authorSource, metadata.getTitle()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadata;
    }

    private boolean isWeakTitle(String title, String fallbackName) {
        if (title == null || title.isBlank()) {
            return true;
        }

        String normalized = title.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 4) {
            return true;
        }

        if (List.of("paper", "document", "untitled", "pdf", "file").contains(normalized)) {
            return true;
        }

        if (fallbackName != null && !fallbackName.isBlank()) {
            String fallbackBase = fallbackName.replaceAll("\\.[Pp][Dd][Ff]$", "")
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .replaceAll("\\s+", " ")
                    .trim()
                    .toLowerCase(Locale.ROOT);
            if (!fallbackBase.isBlank() && normalized.equals(fallbackBase)) {
                return true;
            }
        }

        return false;
    }

    private String extractFirstPageText(PDDocument document) throws Exception {
        if (document == null || document.getNumberOfPages() == 0) {
            return null;
        }

        PDFTextStripper pdfStripper = new PDFTextStripper();
        pdfStripper.setStartPage(1);
        pdfStripper.setEndPage(1);
        return pdfStripper.getText(document);
    }

    private String extractDoi(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = DOI_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group()
                .replaceAll("[.,;:)]+$", "")
                .toLowerCase(Locale.ROOT);
    }

    private String extractTitle(String text, String fallbackName) {
        if (text != null) {
            List<String> titleLines = new ArrayList<>();
            for (String line : text.split("\\R")) {
                String cleaned = line.replaceAll("\\s+", " ").trim();
                if (cleaned.isBlank()) {
                    continue;
                }
                if (looksLikeSectionHeader(cleaned)) {
                    break;
                }
                if (cleaned.length() < 6) {
                    continue;
                }
                titleLines.add(cleaned);
                if (looksLikeAuthorLine(cleaned) && titleLines.size() > 1) {
                    titleLines.remove(titleLines.size() - 1);
                    break;
                }
                if (titleLines.size() == 2) {
                    break;
                }
            }

            String candidate = String.join(" ", titleLines).trim();
            if (candidate.length() > 5) {
                return candidate.length() > 180 ? candidate.substring(0, 180) : candidate;
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

    private List<String> extractAuthors(String text, String title) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> authors = new ArrayList<>();
        String[] lines = text.split("\\R");
        int titleIndex = findTitleLineIndex(lines, title);
        int start = titleIndex >= 0 ? titleIndex + 1 : 0;
        for (int i = start; i < Math.min(lines.length, start + 12); i++) {
            String cleaned = lines[i].replaceAll("\\s+", " ").trim();
            if (cleaned.isBlank()) {
                continue;
            }
            if (looksLikeSectionHeader(cleaned)) {
                break;
            }
            if (looksLikeAuthorLine(cleaned)) {
                authors.addAll(splitAuthorLine(cleaned));
                if (!authors.isEmpty()) {
                    return dedupeAuthors(authors);
                }
                continue;
            }
            if (!authors.isEmpty()) {
                break;
            }
        }

        return List.of();
    }

    private int findTitleLineIndex(String[] lines, String title) {
        if (lines == null || lines.length == 0 || title == null || title.isBlank()) {
            return -1;
        }

        String normalizedTitle = normalizeLine(title);
        for (int i = 0; i < lines.length; i++) {
            String normalizedLine = normalizeLine(lines[i]);
            if (!normalizedLine.isBlank() && normalizedLine.equals(normalizedTitle)) {
                return i;
            }
        }

        for (int i = 0; i < lines.length; i++) {
            String normalizedLine = normalizeLine(lines[i]);
            if (!normalizedLine.isBlank() && normalizedLine.contains(normalizedTitle)) {
                return i;
            }
        }

        return -1;
    }

    private String normalizeLine(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private List<String> parseAuthorField(String authorField) {
        if (authorField == null || authorField.isBlank()) {
            return List.of();
        }

        String[] parts = authorField.split("\\s*(?:and|&|,|;)\\s*");
        List<String> authors = new ArrayList<>();
        for (String part : parts) {
            String author = part.trim();
            if (!author.isBlank()) {
                authors.add(author);
            }
        }
        return dedupeAuthors(authors);
    }

    private List<String> splitAuthorLine(String line) {
        String[] parts = line.split("\\s*(?:and|&|,|;)\\s*");
        List<String> authors = new ArrayList<>();
        for (String part : parts) {
            String author = part.trim();
            if (!author.isBlank() && author.length() > 2) {
                authors.add(author);
            }
        }
        return dedupeAuthors(authors);
    }

    private List<String> dedupeAuthors(List<String> authors) {
        return new ArrayList<>(new LinkedHashSet<>(authors));
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
                && (value.contains(",") || value.contains(" and ") || value.contains("&") || looksLikeNameList(value))
                && !value.matches(".*\\b(university|institute|department|school|college|laboratory)\\b.*");
    }

    private boolean looksLikeNameList(String value) {
        String[] tokens = value.split("\\s+");
        if (tokens.length < 2 || tokens.length > 8) {
            return false;
        }

        int nameLikeTokens = 0;
        for (String token : tokens) {
            String cleaned = token.replaceAll("[^A-Za-zÀ-ÿ'.-]", "");
            if (cleaned.isBlank()) {
                continue;
            }
            if (Character.isUpperCase(cleaned.charAt(0))) {
                nameLikeTokens++;
            }
        }

        return nameLikeTokens >= 2;
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
