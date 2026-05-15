package mk.ukim.finki.literaturereviewassistant.service;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ArticleService {
        List<Article> listArticles();

        Page<Article> find(
                String url,
                String title,
                String doi,
                Long authorId,
                Long documentId,
                Long surveyId,
                Integer pageNum,
                Integer pageSize
        );

        Article findById(Long id);

        Article create(
                String url,
                String title,
                String doi,
                Long authorId,
                Long documentId,
                Long surveyId

        );

        Article update(
                Long id,
                String name,
                Double price,
                Integer quantity,
                ProductLevel level,
                Long categoryId,
                Long manufacturerId
        );

        List<Article> search(String text);

        void delete(Long id);

        void toggleProductStatus(Long id);


}
