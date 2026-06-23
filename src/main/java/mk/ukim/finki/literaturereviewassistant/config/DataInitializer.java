package mk.ukim.finki.literaturereviewassistant.config;


import mk.ukim.finki.literaturereviewassistant.model.*;
import mk.ukim.finki.literaturereviewassistant.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SurveyRepository surveyRepository;
    private final ArticleRepository articleRepository;
    private final AuthorRepository authorRepository;
    private final ReviewerRepository reviewerRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            SurveyRepository surveyRepository,
            ArticleRepository articleRepository,
            AuthorRepository authorRepository,
            ReviewerRepository reviewerRepository,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.surveyRepository = surveyRepository;
        this.articleRepository = articleRepository;
        this.authorRepository = authorRepository;
        this.reviewerRepository = reviewerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@finki.ukim.mk";



        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {

            AppUser admin = new AppUser(
                    null,
                    "System Admin",
                    adminEmail,
                    passwordEncoder.encode("SecureAdminPassword123!"),
                    "ADMIN"
            );
            userRepository.save(admin);
            System.out.println("Creating admin user with email: " + adminEmail);
        } else {
            // Update password just in case it was changed during development
            userRepository.findByEmailIgnoreCase(adminEmail).ifPresent(admin -> {
                admin.setPasswordHash(passwordEncoder.encode("SecureAdminPassword123!"));
                userRepository.save(admin);
            });
        }
        // ── Owner ─────────────────────────────────────────────────────────────────
        String ownerEmail = "owner@finki.ukim.mk";
        if (!userRepository.existsByEmailIgnoreCase(ownerEmail)) {
            AppUser owner = new AppUser(null, "Survey Owner", ownerEmail,
                    passwordEncoder.encode("Owner123!"), "USER");
            userRepository.save(owner);
        } else {
            userRepository.findByEmailIgnoreCase(ownerEmail).ifPresent(owner -> {
                owner.setPasswordHash(passwordEncoder.encode("Owner123!"));
                userRepository.save(owner);
            });
        }

        // ── Reviewer 1 ────────────────────────────────────────────────────────────
        String anaEmail = "ana@finki.ukim.mk";
        if (!userRepository.existsByEmailIgnoreCase(anaEmail)) {
            AppUser ana = new AppUser(null, "Ana Petrova", anaEmail,
                    passwordEncoder.encode("Reviewer123!"), "USER");
            userRepository.save(ana);
        } else {
            userRepository.findByEmailIgnoreCase(anaEmail).ifPresent(ana -> {
                ana.setPasswordHash(passwordEncoder.encode("Reviewer123!"));
                userRepository.save(ana);
            });
        }

        // ── Reviewer 2 ────────────────────────────────────────────────────────────
        String markEmail = "mark@finki.ukim.mk";
        if (!userRepository.existsByEmailIgnoreCase(markEmail)) {
            AppUser mark = new AppUser(null, "Mark Johnson", markEmail,
                    passwordEncoder.encode("Reviewer123!"), "USER");
            userRepository.save(mark);
        } else {
            userRepository.findByEmailIgnoreCase(markEmail).ifPresent(mark -> {
                mark.setPasswordHash(passwordEncoder.encode("Reviewer123!"));
                userRepository.save(mark);
            });
        }

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

//        AppUser ownerUser = createMockUser("Survey Owner", "owner@example.com", "USER");
//        AppUser anaUser = createMockUser("Ana Petrova", "ana.petrova@example.com", "USER");
//        AppUser markUser = createMockUser("Mark Johnson", "mark.johnson@example.com", "USER");
        AppUser ownerUser = userRepository.findByEmailIgnoreCase("owner@finki.ukim.mk").orElseThrow();
        AppUser anaUser   = userRepository.findByEmailIgnoreCase("ana@finki.ukim.mk").orElseThrow();
        AppUser markUser  = userRepository.findByEmailIgnoreCase("mark@finki.ukim.mk").orElseThrow();

        addDefaultReviewers(List.of(healthcare, climate, remoteWork), ownerUser, anaUser, markUser);

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
                "This paper explores the ethical implications of dep    loying AI-driven clinical decision support systems.",
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

//    private AppUser createMockUser(String name, String email, String role) {
//        if (!userRepository.existsByEmailIgnoreCase(email)) {
//            AppUser user = new AppUser(
//                    null,
//                    name,
//                    email,
//                    passwordEncoder.encode("Password123!"),
//                    role
//            );
//            return userRepository.save(user);
//        }
//        return userRepository.findByEmailIgnoreCase(email).orElseThrow();
//    }

    private void addDefaultReviewers(List<Survey> surveys, AppUser ownerUser, AppUser anaUser, AppUser markUser) {
        // 1. Create the Owner
        Reviewer owner = new Reviewer();
        owner.setExternalId("owner");
        owner.setName("Survey Owner");
        owner.setEmail("owner@finki.ukim.mk");
        owner.setRole("Owner");
        owner.setAddedDate(Instant.now());
        owner.setSurveys(new ArrayList<>(surveys));
        owner.setAppUser(ownerUser);
        reviewerRepository.save(owner);

        // 2. Create Default Reviewer 1
        Reviewer reviewer1 = new Reviewer();
        reviewer1.setExternalId("reviewer-1");
        reviewer1.setName("Ana Petrova");
        reviewer1.setEmail("ana@finki.ukim.mk");
        reviewer1.setRole("Reviewer");
        reviewer1.setAddedDate(Instant.now());
        reviewer1.setSurveys(new ArrayList<>(surveys));
        reviewer1.setAppUser(anaUser);
        reviewerRepository.save(reviewer1);

        // 3. Create Default Reviewer 2
        Reviewer reviewer2 = new Reviewer();
        reviewer2.setExternalId("reviewer-2");
        reviewer2.setName("Mark Johnson");
        reviewer2.setEmail("mark@finki.ukim.mk");
        reviewer2.setRole("Reviewer");
        reviewer2.setAddedDate(Instant.now());
        reviewer2.setSurveys(new ArrayList<>(surveys));
        reviewer2.setAppUser(markUser);
        reviewerRepository.save(reviewer2);
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
