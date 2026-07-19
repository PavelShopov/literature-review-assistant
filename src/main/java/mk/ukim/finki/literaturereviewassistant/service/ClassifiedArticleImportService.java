package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.web.dto.ClassifiedArticleImportSummary;
import org.springframework.web.multipart.MultipartFile;

public interface ClassifiedArticleImportService {
    ClassifiedArticleImportSummary importClassifiedArticles(String surveyId, MultipartFile file);
}
