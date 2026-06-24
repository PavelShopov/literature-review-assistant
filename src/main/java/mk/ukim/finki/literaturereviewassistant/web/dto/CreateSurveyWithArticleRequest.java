package mk.ukim.finki.literaturereviewassistant.web.dto;

public class CreateSurveyWithArticleRequest {
    private String name;
    private String initialArticleId;

    public CreateSurveyWithArticleRequest() {}

    public CreateSurveyWithArticleRequest(String name, String initialArticleId) {
        this.name = name;
        this.initialArticleId = initialArticleId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInitialArticleId() { return initialArticleId; }
    public void setInitialArticleId(String initialArticleId) { this.initialArticleId = initialArticleId; }
}