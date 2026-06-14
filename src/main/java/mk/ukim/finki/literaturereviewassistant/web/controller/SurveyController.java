package mk.ukim.finki.literaturereviewassistant.web.controller;

import mk.ukim.finki.literaturereviewassistant.model.AppUser;
import mk.ukim.finki.literaturereviewassistant.model.Article;
import mk.ukim.finki.literaturereviewassistant.model.Survey;
import mk.ukim.finki.literaturereviewassistant.model.SurveyContributor;
import mk.ukim.finki.literaturereviewassistant.repository.AppUserRepository;
import mk.ukim.finki.literaturereviewassistant.repository.ArticleRepository;
import mk.ukim.finki.literaturereviewassistant. repository.SurveyContributorRepository;
import mk.ukim.finki.literaturereviewassistant.repository.SurveyRepository;
import mk.ukim.finki.literaturereviewassistant.web.dto.ArticleDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.ContributorDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDetailsDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.SurveyDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/surveys")
@CrossOrigin(origins = "http://localhost:5173")
public class SurveyController {

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyContributorRepository contributorRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @GetMapping
    public List<SurveyDto> getAllSurveys() {
        return surveyRepository.findAll().stream().map(this::toSurveyDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurveyDetailsDto> getSurveyById(@PathVariable Long id) {
        return surveyRepository.findById(id)
                .map(survey -> ResponseEntity.ok(toSurveyDetailsDto(survey)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SurveyDto> saveSurvey(@PathVariable String id, @RequestBody SurveyDto dto) {
        Survey survey;
        if (id.startsWith("new-") || dto.getId() == null) {
            survey = new Survey();
            survey.setCreatedDate(LocalDate.now());
            survey.setStatus("In Progress");
        } else {
            survey = surveyRepository.findById(Long.parseLong(id)).orElse(new Survey());
        }
        
        survey.setTitle(dto.getName());
        survey.setDescription(dto.getDescription());
        if (dto.getStatus() != null) survey.setStatus(dto.getStatus());
        
        Survey saved = surveyRepository.save(survey);
        return ResponseEntity.ok(toSurveyDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long id) {
        surveyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/articles/{articleId}")
    public ResponseEntity<ArticleDto> getArticleById(@PathVariable Long articleId) {
        return articleRepository.findById(articleId)
                .map(this::toArticleDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Contributors ---
    
    @GetMapping("/{surveyId}/contributors")
    public List<ContributorDto> getContributors(@PathVariable Long surveyId) {
        return contributorRepository.findBySurvey_SurveyId(surveyId).stream()
                .map(this::toContributorDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/{surveyId}/contributors")
    public ResponseEntity<ContributorDto> addContributor(@PathVariable Long surveyId, @RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String role = payload.getOrDefault("role", "Reviewer");

        Optional<Survey> surveyOpt = surveyRepository.findById(surveyId);
        Optional<AppUser> userOpt = userRepository.findByEmail(email);

        if (surveyOpt.isPresent() && userOpt.isPresent()) {
            SurveyContributor contributor = new SurveyContributor();
            contributor.setSurvey(surveyOpt.get());
            contributor.setUser(userOpt.get());
            contributor.setRole(role);
            contributor.setAddedDate(java.time.LocalDateTime.now());
            
            SurveyContributor saved = contributorRepository.save(contributor);
            return ResponseEntity.ok(toContributorDto(saved));
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{surveyId}/contributors/{contributorId}")
    public ResponseEntity<Void> removeContributor(@PathVariable Long surveyId, @PathVariable Long contributorId) {
        contributorRepository.deleteById(contributorId);
        return ResponseEntity.noContent().build();
    }

    // --- Mappers ---

    private SurveyDto toSurveyDto(Survey survey) {
        SurveyDto dto = new SurveyDto();
        dto.setId(survey.getSurveyId() != null ? survey.getSurveyId().toString() : null);
        dto.setName(survey.getTitle());
        dto.setDescription(survey.getDescription());
        dto.setCreatedDate(survey.getCreatedDate() != null ? survey.getCreatedDate().toString() : null);
        dto.setStatus(survey.getStatus());
        dto.setTotalArticles(survey.getArticles() != null ? survey.getArticles().size() : 0);
        return dto;
    }

    private SurveyDetailsDto toSurveyDetailsDto(Survey survey) {
        SurveyDetailsDto dto = new SurveyDetailsDto();
        dto.setId(survey.getSurveyId() != null ? survey.getSurveyId().toString() : null);
        dto.setName(survey.getTitle());
        dto.setDescription(survey.getDescription());
        dto.setCreatedDate(survey.getCreatedDate() != null ? survey.getCreatedDate().toString() : null);
        dto.setStatus(survey.getStatus());
        dto.setTotalArticles(survey.getArticles() != null ? survey.getArticles().size() : 0);
        dto.setResearchQuestion(survey.getResearchQuestion());
        dto.setScreened(0); // Add real logic if available
        dto.setPending(survey.getArticles() != null ? survey.getArticles().size() : 0);
        
        List<SurveyContributor> contributors = contributorRepository.findBySurvey_SurveyId(survey.getSurveyId());
        contributors.stream().filter(c -> "Owner".equals(c.getRole())).findFirst().ifPresent(owner -> {
            SurveyDetailsDto.OwnerDto ownerDto = new SurveyDetailsDto.OwnerDto();
            ownerDto.setName(owner.getUser().getName());
            ownerDto.setEmail(owner.getUser().getEmail());
            dto.setOwner(ownerDto);
        });
        
        return dto;
    }

    private ContributorDto toContributorDto(SurveyContributor contributor) {
        ContributorDto dto = new ContributorDto();
        dto.setId(contributor.getId().toString());
        dto.setName(contributor.getUser().getName());
        dto.setEmail(contributor.getUser().getEmail());
        dto.setRole(contributor.getRole());
        dto.setAddedDate(contributor.getAddedDate().toString());
        return dto;
    }

    private ArticleDto toArticleDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setId(article.getArticleId() != null ? article.getArticleId().toString() : null);
        dto.setTitle(article.getTitle());
        dto.setJournal(article.getJournal());
        dto.setYear(article.getYear());
        dto.setDoi(article.getDoi());
        dto.setStatus(article.getStatus() != null ? article.getStatus() : "PENDING");
        dto.setAbstractText(article.getArticleAbstract());
        dto.setInclusionSummary(article.getInclusionSummary());
        
        List<String> authorNames = new ArrayList<>();
        if (article.getAuthors() != null) {
            article.getAuthors().forEach(a -> authorNames.add(a.getFullName()));
        }
        dto.setAuthors(authorNames);
        
        if (article.getAddedBy() != null) {
            ArticleDto.AddedByDto addedByDto = new ArticleDto.AddedByDto();
            addedByDto.setId(article.getAddedBy().getId().toString());
            addedByDto.setName(article.getAddedBy().getName());
            // Need to figure out role based on contributor, assuming Reviewer for simplicity
            addedByDto.setRole("Reviewer"); 
            dto.setAddedBy(addedByDto);
        }
        
        return dto;
    }
}