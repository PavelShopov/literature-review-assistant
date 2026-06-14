package mk.ukim.finki.literaturereviewassistant.service.DataService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

@Service
public class PdfExtractorService {
    public ArticleMetadata extractFromUrl(String url) {
        ArticleMetadata metadata = new ArticleMetadata();
        try (InputStream in = new URL(url).openStream()) {
             byte[] bytes = in.readAllBytes();
             metadata = extractFromBytes(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadata;
    }

    public ArticleMetadata extractFromBytes(byte[] bytes) {
        ArticleMetadata metadata = new ArticleMetadata();
        metadata.setPdfBytes(bytes);
        
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            metadata.setAbstractText(text.substring(0, Math.min(text.length(), 2000)));

            PDDocumentInformation info = document.getDocumentInformation();
            if (info != null) {
                if (info.getTitle() != null) metadata.setTitle(info.getTitle());
                if (info.getAuthor() != null) {
                    metadata.setAuthorNames(Arrays.asList(info.getAuthor().split(",")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadata;
    }
}
