package mk.ukim.finki.literaturereviewassistant.config;

import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Author;
import mk.ukim.finki.literaturereviewassistant.model.Reviewer;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleRepository;
import mk.ukim.finki.literaturereviewassistant.repository.AuthorRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ReviewerRepository;
import mk.ukim.finki.literaturereviewassistant.repository.SurveyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SurveyRepository surveyRepository;
    private final ArticleRepository articleRepository;
    private final AuthorRepository authorRepository;
    private final ReviewerRepository reviewerRepository;

    public DataInitializer(
            SurveyRepository surveyRepository,
            ArticleRepository articleRepository,
            AuthorRepository authorRepository,
            ReviewerRepository reviewerRepository
    ) {
        this.surveyRepository = surveyRepository;
        this.articleRepository = articleRepository;
        this.authorRepository = authorRepository;
        this.reviewerRepository = reviewerRepository;
    }

    @Override
    public void run(String... args) {
        if (surveyRepository.count() > 0) {
            return;
        }

        Survey healthcare = createSurvey(
                "survey-001",
                "Impact of AI on Healthcare Outcomes",
                "A systematic literature review examining the effectiveness and implementation of artificial intelligence technologies in healthcare settings.",
                "How effective are AI technologies in improving patient outcomes and clinical decision-making in healthcare environments?",
                "In Progress",
                LocalDate.of(2024, 3, 15)
        );

        Survey climate = createSurvey(
                "survey-002",
                "Climate Change and Agricultural Productivity",
                "Analyzing the relationship between climate change factors and crop yields across different geographic regions.",
                "How do changes in temperature, rainfall, and extreme weather affect crop productivity across regions?",
                "In Progress",
                LocalDate.of(2024, 2, 20)
        );

        Survey remoteWork = createSurvey(
                "survey-003",
                "Remote Work and Employee Wellbeing",
                "Investigating the psychological and productivity impacts of remote work arrangements post-pandemic.",
                "What measurable effects does remote work have on employee wellbeing, collaboration, and productivity?",
                "Completed",
                LocalDate.of(2024, 1, 10)
        );

        surveyRepository.saveAll(List.of(healthcare, climate, remoteWork));

        addDefaultReviewers(healthcare);
        addDefaultReviewers(climate);
        addDefaultReviewers(remoteWork);

        createArticle(healthcare, "1", "Deep Learning Applications in Medical Image Analysis: A Systematic Review",
                List.of("Smith, J.", "Johnson, A.", "Williams, B."), "Journal of Medical Imaging", 2024,
                "10.1000/jmi.2024.001", "INCLUDED",
                "This systematic review examines the current state of deep learning applications in medical image analysis.",
                "owner", "Survey Owner", "Owner");
        createArticle(healthcare, "2", "Machine Learning in Healthcare: Opportunities and Challenges",
                List.of("Chen, L.", "Rodriguez, M."), "Nature Medicine", 2023,
                "10.1038/nm.2023.456", "PENDING",
                "We present a comprehensive analysis of machine learning applications in healthcare settings.",
                "reviewer-1", "Ana Petrova", "Reviewer");
        createArticle(healthcare, "3", "Ethical Considerations in AI-Driven Clinical Decision Support Systems",
                List.of("Kumar, R.", "Thompson, E.", "Lee, S.", "Davis, K."), "The Lancet Digital Health", 2024,
                "10.1016/s2589-7500(24)00012-3", "INCLUDED",
                "This paper explores the ethical implications of deploying AI-driven clinical decision support systems.",
                "reviewer-2", "Mark Johnson", "Reviewer");
    }

    private Survey createSurvey(String id, String title, String description, String researchQuestion, String status, LocalDate createdDate) {
        Survey survey = new Survey();
        survey.setExternalId(id);
        survey.setTitle(title);
        survey.setDescription(description);
        survey.setResearchQuestion(researchQuestion);
        survey.setStatus(status);
        survey.setCreatedDate(createdDate);
        return survey;
    }

    private void addDefaultReviewers(Survey survey) {
        reviewerRepository.save(new Reviewer(null, "owner", "Survey Owner", "owner@example.com", "Owner", Instant.now(), survey));
        reviewerRepository.save(new Reviewer(null, "reviewer-1", "Ana Petrova", "ana.petrova@example.com", "Reviewer", Instant.now(), survey));
        reviewerRepository.save(new Reviewer(null, "reviewer-2", "Mark Johnson", "mark.johnson@example.com", "Reviewer", Instant.now(), survey));
    }

    private void createArticle(
            Survey survey,
            String id,
            String title,
            List<String> authorNames,
            String journal,
            int year,
            String doi,
            String status,
            String abstractText,
            String addedById,
            String addedByName,
            String addedByRole
    ) {
        Article article = new Article();
        article.setExternalId(id);
        article.setTitle(title);
        article.setAuthors(authorNames.stream().map(this::resolveAuthor).toList());
        article.setJournal(journal);
        article.setPublicationYear(year);
        article.setDoi(doi);
        article.setStatus(status);
        article.setSurveyExternalId(survey.getExternalId());
        article.setArticleAbstract(abstractText);
        article.setAddedById(addedById);
        article.setAddedByName(addedByName);
        article.setAddedByRole(addedByRole);
        articleRepository.save(article);
    }

    private Author resolveAuthor(String name) {
        return authorRepository.findByAuthorName(name).orElseGet(() -> {
            Author author = new Author();
            author.setAuthorName(name);
            return authorRepository.save(author);
        });
    }
}
