package mk.ukim.finki.literaturereviewassistant.web.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassifiedArticleImportSummary {
    private int articlesCreated;
    private int articlesUpdated;
    private int articlesSkipped;
    private int surveyLinksCreated;
    private int surveyLinksSkipped;
    private int classificationsCreated;
    private int classificationsUpdated;
    private int classificationsSkipped;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private final Map<String, Integer> warningCounts = new LinkedHashMap<>();

    public int getArticlesCreated() {
        return articlesCreated;
    }

    public void incrementArticlesCreated() {
        this.articlesCreated++;
    }

    public int getArticlesUpdated() {
        return articlesUpdated;
    }

    public void incrementArticlesUpdated() {
        this.articlesUpdated++;
    }

    public int getArticlesSkipped() {
        return articlesSkipped;
    }

    public void incrementArticlesSkipped() {
        this.articlesSkipped++;
    }

    public int getSurveyLinksCreated() {
        return surveyLinksCreated;
    }

    public void incrementSurveyLinksCreated() {
        this.surveyLinksCreated++;
    }

    public int getSurveyLinksSkipped() {
        return surveyLinksSkipped;
    }

    public void incrementSurveyLinksSkipped() {
        this.surveyLinksSkipped++;
    }

    public int getClassificationsCreated() {
        return classificationsCreated;
    }

    public void incrementClassificationsCreated() {
        this.classificationsCreated++;
    }

    public int getClassificationsUpdated() {
        return classificationsUpdated;
    }

    public void incrementClassificationsUpdated() {
        this.classificationsUpdated++;
    }

    public int getClassificationsSkipped() {
        return classificationsSkipped;
    }

    public void incrementClassificationsSkipped() {
        this.classificationsSkipped++;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public void incrementWarningCount(String warningKey) {
        warningCounts.merge(warningKey, 1, Integer::sum);
    }

    public void finalizeWarnings() {
        if (!warningCounts.isEmpty()) {
            List<String> aggregatedWarnings = new ArrayList<>();
            warningCounts.forEach((warningKey, count) -> aggregatedWarnings.add(count + " " + warningKey));
            aggregatedWarnings.addAll(warnings);
            warnings = aggregatedWarnings;
        }
    }
}
