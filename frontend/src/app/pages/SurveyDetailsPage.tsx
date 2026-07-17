import React, { useEffect, useState, useMemo } from "react";
import { useNavigate, useParams } from "react-router";
import {
  FileText,
  Users,
  Calendar,
  ArrowLeft,
  LayoutDashboard,
  Sparkles,
  ArrowRight,
  Filter,
  UsersRound,
  Sliders,
  Save,
  CheckSquare,
  Square, Plus,
  X,
  Upload
} from "lucide-react";
import { toast, Toaster } from "sonner";
import { AskInput } from "../components/AskInput";
import { LoadingState } from "../components/LoadingState";
import { AIAnswer } from "../components/AIAnswer";
import { ReferencesList, type Reference } from "../components/ReferencesList";
import { EmptyAskState } from "../components/EmptyAskState";
import { ArticleCard, type Article, type ArticleStatus } from "../components/ArticleCard";
import { ImportSection } from "../components/ImportSection";
import { EmptyState } from "../components/EmptyState";
import { ContributorsPanel, type Contributor } from "../components/ContributorsPanel";
import { useAuth } from "../auth/AuthContext";
import {
  addContributor,
  askSurveyQuestion,
  deleteSurveyArticle,
  getContributors,
  getSurvey,
  getSurveyArticles,
  importSurveyArticle,
  openArticleResource,
  mockArticles,
  removeContributor,
  updateSurveyArticle,
  type SurveyDetails,
  createSurveyWithArticle,
  importClassifiedArticles,
  importTaxonomy,
  type ClassifiedArticleImportSummary,
  type TaxonomyImportSummary,
} from "../api/client";

type TabType = "dashboard" | "articles" | "ask-ai" | "reviewers";
type ArticleFilter = "all" | "screened" | "pending";
type ReferenceDecision = {
  decision: "added" | "skipped";
  summary: string;
  decidedAt: string;
  reviewerName?: string;
};

// All custom dimensions requested for the literature extraction protocol
const EXTRACTION_CRITERIA_OPTIONS = [
  { id: "methodology", label: "Research Methodology", group: "Technical" },
  { id: "dataset_context", label: "Target Dataset Context", group: "Data" },
  { id: "framework_approach", label: "Model / Framework Approach", group: "Technical" },
  { id: "evaluation_metrics", label: "Evaluation Metrics Utilized", group: "Technical" },
  { id: "data_sourcing", label: "Data Sourcing Type", group: "Data" },
  { id: "research_focus", label: "Research Focus Area", group: "Domain" },
  { id: "application_domain", label: "Primary Application Domain", group: "Domain" },
  { id: "computing_env", label: "Computing Environment", group: "Infrastructure" },
  { id: "open_science", label: "Code Availability & Open Science", group: "Infrastructure" },
  { id: "learning_paradigm", label: "Learning Paradigm", group: "Technical" },
  { id: "model_size", label: "Scale of Parameters / Model Size", group: "Technical" },
  { id: "hardware_reqs", label: "Hardware Requirements", group: "Infrastructure" },
  { id: "limitations", label: "Limitations Acknowledged", group: "Domain" },
  { id: "funding_source", label: "Funding Source Type", group: "Metadata" },
  { id: "target_audience", label: "Target Audience / Stakeholder", group: "Metadata" },
];

const normalizeReviewers = (items: Contributor[]): Contributor[] =>
    items.map((item) => ({
      ...item,
      role: item.role === "Owner" ? "Owner" : "Reviewer",
    }));

const stopWords = new Set([
  "the", "and", "for", "with", "from", "into", "that", "this", "what", "when",
  "where", "which", "how", "why", "about", "your", "survey", "article",
  "articles", "study", "studies", "question"
]);

const tokenize = (value: string) => value.toLowerCase().match(/[a-z0-9]+/g) ?? [];

const scoreArticle = (article: Article, question: string): number => {
  const queryTerms = tokenize(question).filter((term) => term.length > 2 && !stopWords.has(term));
  const haystack = [
    article.title,
    article.authors.join(" "),
    article.journal,
    article.abstract ?? "",
    article.inclusionSummary ?? "",
    article.doi,
  ]
      .join(" ")
      .toLowerCase();

  if (queryTerms.length === 0) {
    return article.status === "INCLUDED" ? 6 : article.status === "PENDING" ? 4 : 2;
  }

  let score = 0;
  for (const term of queryTerms) {
    if (haystack.includes(term)) {
      score += 3;
    }
  }

  if (article.title.toLowerCase().includes(queryTerms[0] ?? "")) {
    score += 2;
  }

  if (article.status === "INCLUDED") {
    score += 2;
  } else if (article.status === "PENDING") {
    score += 1;
  }

  return score;
};

const buildTopReferences = (articles: Article[], question: string): Reference[] => {
  const ranked = [...articles]
      .map((article) => ({
        article,
        score: scoreArticle(article, question),
      }))
      .sort((left, right) => right.score - left.score || right.article.year - left.article.year)
      .slice(0, 5);

  const maxScore = Math.max(1, ...ranked.map((item) => item.score));

  return ranked.map(({ article, score }) => ({
    id: article.id,
    title: article.title,
    authors: article.authors,
    journal: article.journal,
    year: article.year,
    snippet:
        article.inclusionSummary?.trim() ||
        article.abstract?.trim() ||
        "No abstract available for this article.",
    relevanceScore: Math.max(20, Math.round((score / maxScore) * 100)),
  }));
};

interface CustomDimension {
  name: string;
  options: string[];
}

export default function SurveyDetailsPage() {
  const navigate = useNavigate();
  const { surveyId = "survey-001" } = useParams();
  const { user: authUser } = useAuth();
  const roleParam = new URLSearchParams(window.location.search).get("role");

  const [activeTab, setActiveTab] = useState<TabType>("dashboard");
  const [articleFilter, setArticleFilter] = useState<ArticleFilter>("all");

  // Ask AI states
  const [isLoading, setIsLoading] = useState(false);
  const [currentQuestion, setCurrentQuestion] = useState<string | null>(null);
  const [currentAnswer, setCurrentAnswer] = useState<string | null>(null);
  const [showResults, setShowResults] = useState(false);

  // Articles states
  const [articles, setArticles] = useState<Article[]>(mockArticles);
  const [isImportOpen, setIsImportOpen] = useState(false);
  const [contributors, setContributors] = useState<Contributor[]>([]);
  const [referenceDecisions, setReferenceDecisions] = useState<Record<string, ReferenceDecision>>({});

  const [survey, setSurvey] = useState<SurveyDetails | null>(null);
  const [surveyError, setSurveyError] = useState<string | null>(null);

  // Custom metadata criteria setup states
  const [selectedCriteria, setSelectedCriteria] = useState<string[]>([]);

  const [customDimensions, setCustomDimensions] = useState<CustomDimension[]>([]);
  const [dimensionInput, setDimensionInput] = useState("");

  const [optionInput, setOptionInput] = useState("");
  const [currentOptionsBuild, setCurrentOptionsBuild] = useState<string[]>([]);
  const [taxonomyFile, setTaxonomyFile] = useState<File | null>(null);
  const [isImportingTaxonomy, setIsImportingTaxonomy] = useState(false);
  const [taxonomyImportError, setTaxonomyImportError] = useState<string | null>(null);
  const [taxonomyImportSummary, setTaxonomyImportSummary] = useState<TaxonomyImportSummary | null>(null);
  const [classifiedArticlesFile, setClassifiedArticlesFile] = useState<File | null>(null);
  const [isImportingClassifiedArticles, setIsImportingClassifiedArticles] = useState(false);
  const [classifiedArticlesImportError, setClassifiedArticlesImportError] = useState<string | null>(null);
  const [classifiedArticlesImportSummary, setClassifiedArticlesImportSummary] =
      useState<ClassifiedArticleImportSummary | null>(null);

  const defaultOwner = {
    id: "owner",
    name: "Survey Owner",
    email: "owner@example.com",
    role: "Owner" as const,
    addedDate: new Date().toISOString(),
  };
  const defaultReviewer = {
    id: "reviewer-1",
    name: "Ana Petrova",
    email: "ana.petrova@example.com",
    role: "Reviewer" as const,
    addedDate: new Date().toISOString(),
  };

  const currentUser = authUser ?? (isReviewerFallback(roleParam) ? defaultReviewer : defaultOwner);

  const currentRole: "Owner" | "Reviewer" = authUser
      ? contributors.some(
          (c) => c.email?.toLowerCase() === authUser.email?.toLowerCase() && c.role === "Owner",
      ) || (survey?.owner?.email?.toLowerCase() === authUser.email?.toLowerCase())
          ? "Owner"
          : "Reviewer"
      : isReviewerFallback(roleParam)
          ? "Reviewer"
          : "Owner";

  const isOwnerView = currentRole === "Owner";
  const totalArticles = articles.length;
  const screenedArticles = articles.filter(
      (article) => article.status === "INCLUDED" || article.status === "EXCLUDED",
  ).length;
  const pendingArticles = articles.filter((article) => article.status === "PENDING").length;

  const topReferences = useMemo(
      () => buildTopReferences(articles, currentQuestion ?? ""),
      [articles, currentQuestion],
  );

  async function loadSurveyData() {
    const storageKey = `survey:${surveyId}:contributors`;

    try {
      const [surveyData, surveyArticles, surveyContributors] = await Promise.all([
        getSurvey(surveyId),
        getSurveyArticles(surveyId).catch(() => []),
        getContributors(surveyId).catch(() => []),
      ]);

      setSurvey(surveyData);
      setArticles(surveyArticles);

      if (surveyData && (surveyData as any).criteria) {
        const dbCriteria = (surveyData as any).criteria;
        const normalized = dbCriteria.map((item: any) =>
            typeof item === 'string' ? { name: item, options: [] } : item
        );
        setCustomDimensions(normalized);
      } else {
        const savedDimensions = localStorage.getItem(`survey:${surveyId}:dimensions`);
        if (savedDimensions) {
          setCustomDimensions(JSON.parse(savedDimensions));
        } else {
          setCustomDimensions([
            { name: "Research Methodology", options: ["Empirical Study", "Theoretical Analysis", "System Design", "Literature Review"] },
            { name: "Model / Framework Approach", options: ["Deep Learning (CNN/Transformer)", "Classical ML", "Reinforcement Learning"] }
          ]);
        }
      }

      const nextContributors =
          surveyContributors.length > 0 ? normalizeReviewers(surveyContributors) : [defaultOwner];

      setContributors(nextContributors);
      localStorage.setItem(storageKey, JSON.stringify(nextContributors));
      setSurveyError(null);
    } catch (err) {
      console.error("Survey data fetch error:", err);
      setSurveyError("This survey could not be loaded. Make sure the backend is running.");
    }
  }

  useEffect(() => {
    setSurvey(null);
    setArticles([]);
    setContributors([]);
    setSurveyError(null);
    void loadSurveyData();

    const timer = window.setInterval(() => {
      void loadSurveyData();
    }, 5000);

    return () => window.clearInterval(timer);
  }, [surveyId]);

  useEffect(() => {
    const storageKey = `survey:${surveyId}:reference-decisions`;
    const stored = localStorage.getItem(storageKey);
    setReferenceDecisions(stored ? JSON.parse(stored) : {});
  }, [surveyId]);

  const persistContributors = (nextContributors: Contributor[]) => {
    setContributors(nextContributors);
    localStorage.setItem(`survey:${surveyId}:contributors`, JSON.stringify(nextContributors));
  };

  const persistReferenceDecisions = (nextDecisions: Record<string, ReferenceDecision>) => {
    setReferenceDecisions(nextDecisions);
    localStorage.setItem(`survey:${surveyId}:reference-decisions`, JSON.stringify(nextDecisions));
  };

  const handleToggleCriteria = (id: string) => {
    if (!isOwnerView) {
      toast.error("Only the survey administrator can edit analysis parameters");
      return;
    }

    let updatedCriteria: string[];
    if (selectedCriteria.includes(id)) {
      updatedCriteria = selectedCriteria.filter((item) => item !== id);
    } else {
      updatedCriteria = [...selectedCriteria, id];
    }

    setSelectedCriteria(updatedCriteria);
    localStorage.setItem(`survey:${surveyId}:criteria`, JSON.stringify(updatedCriteria));
    toast.success("Extraction parameters updated");
  };

  const [savingCriteria, setSavingCriteria] = useState<boolean>(false);
  const handleSaveCriteriaToDatabase = async () => {
    try {
      setSavingCriteria(true);

      // 🟢 Send the whole array object structure containing both .name and .options arrays
      const payload = {
        criteria: customDimensions
      };

      const response = await fetch(`http://localhost:8080/api/surveys/${surveyId}/criteria`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      if (!response.ok) throw new Error(`Status ${response.status}`);
      toast.success("Full criteria blueprint saved to database!");
    } catch (err: any) {
      toast.error("Database Sync Failed");
    } finally {
      setSavingCriteria(false);
    }
  };

  // 🟢 Add option tag to the temporary staging array before finalizing dimension
  const handleAddOptionToStaging = () => {
    const trimmed = optionInput.trim();
    if (!trimmed) return;
    if (currentOptionsBuild.some(o => o.toLowerCase() === trimmed.toLowerCase())) {
      toast.error("Option already added.");
      return;
    }
    setCurrentOptionsBuild([...currentOptionsBuild, trimmed]);
    setOptionInput("");
  };

  const handleRemoveOptionFromStaging = (optToRemove: string) => {
    setCurrentOptionsBuild(currentOptionsBuild.filter(o => o !== optToRemove));
  };

  const handleAddDimension = () => {
    if (!isOwnerView) return;

    const nameTrimmed = dimensionInput.trim();
    if (!nameTrimmed) {
      toast.error("Dimension name cannot be empty");
      return;
    }

    if (customDimensions.some(d => d.name.toLowerCase() === nameTrimmed.toLowerCase())) {
      toast.error("This dimension already exists.");
      return;
    }

    const newDimension: CustomDimension = {
      name: nameTrimmed,
      options: currentOptionsBuild.length > 0 ? currentOptionsBuild : []
    };

    const updated = [...customDimensions, newDimension];
    setCustomDimensions(updated);
    localStorage.setItem(`survey:${surveyId}:dimensions`, JSON.stringify(updated));

    setDimensionInput("");
    setCurrentOptionsBuild([]);
    toast.success(`Dimension "${nameTrimmed}" added with ${newDimension.options.length} choices!`);
  };

  const handleRemoveDimension = (nameToRemove: string) => {
    if (!isOwnerView) return;
    const updated = customDimensions.filter(d => d.name !== nameToRemove);
    setCustomDimensions(updated);
    localStorage.setItem(`survey:${surveyId}:dimensions`, JSON.stringify(updated));
    toast.success("Dimension removed");
  };

  const handleClearAllCriteria = () => {
    if (!isOwnerView) return;
    setCustomDimensions([]);
    localStorage.setItem(`survey:${surveyId}:dimensions`, JSON.stringify([]));
    toast.success("All custom dimensions cleared");
  };

  if (surveyError) {
    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center px-6">
          <div className="max-w-md rounded-xl border border-gray-200 bg-white p-6 text-center shadow-sm">
            <h1 className="text-lg font-semibold text-gray-900 mb-2">Survey failed to load</h1>
            <p className="text-sm text-gray-600 mb-4">{surveyError}</p>
            <button
                onClick={() => navigate("/survey")}
                className="inline-flex items-center justify-center px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              Back to surveys
            </button>
          </div>
        </div>
    );
  }

  if (!survey) {
    return (
        <div className="flex justify-center items-center h-screen bg-gray-50">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <span className="ml-3 text-lg text-gray-600">Loading survey details...</span>
        </div>
    );
  }

  const handleAsk = async (question: string) => {
    setCurrentQuestion(question);
    setIsLoading(true);
    setShowResults(false);

    askSurveyQuestion(surveyId, question)
        .then((answer) => {
          setCurrentAnswer(answer);
          setShowResults(true);
        })
        .catch((err) => {
          toast.error(err instanceof Error ? err.message : "Ask AI failed");
        })
        .finally(() => {
          setIsLoading(false);
        });
  };

  const handleAddContributor = (contributorData: Pick<Contributor, "name" | "email">) => {
    if (!isOwnerView) {
      toast.error("Only the survey owner can add reviewers");
      return;
    }

    if (!contributorData.name || !contributorData.email) {
      toast.error("Add a reviewer name and email");
      return;
    }

    const alreadyAdded = contributors.some(
        (contributor) => contributor.email.toLowerCase() === contributorData.email.toLowerCase(),
    );

    if (alreadyAdded) {
      toast.error("That reviewer is already on this survey");
      return;
    }

    const newContributor: Contributor = {
      id: `reviewer-${Date.now()}`,
      ...contributorData,
      role: "Reviewer",
      addedDate: new Date().toISOString(),
    };
    const nextContributors = [...contributors, newContributor];

    persistContributors(nextContributors);
    addContributor(surveyId, newContributor).catch(() => undefined);
    void loadSurveyData();
    toast.success("Reviewer added to this survey");
  };

  const handleRemoveContributor = (contributorId: string) => {
    if (!isOwnerView) {
      toast.error("Only the survey owner can remove reviewers");
      return;
    }

    const contributor = contributors.find((item) => item.id === contributorId);
    if (contributor?.role === "Owner") return;

    const nextContributors = contributors.filter((item) => item.id !== contributorId);
    persistContributors(nextContributors);
    removeContributor(surveyId, contributorId).catch(() => undefined);
    void loadSurveyData();
    toast.success("Reviewer removed");
  };

  const handleReferenceDecision = (
      referenceId: string,
      decision: ReferenceDecision["decision"],
      summary: string,
  ) => {
    const article = articles.find((item) => item.id === referenceId);
    if (!article) {
      toast.error("Reference is no longer available");
      return;
    }

    const nextDecisions = {
      ...referenceDecisions,
      [referenceId]: {
        decision,
        summary,
        decidedAt: new Date().toISOString(),
        reviewerName: currentUser.name,
      },
    };

    persistReferenceDecisions(nextDecisions);

    const nextStatus: ArticleStatus = decision === "added" ? "INCLUDED" : "EXCLUDED";

    updateSurveyArticle(surveyId, referenceId, {
      title: article.title,
      authors: article.authors,
      journal: article.journal,
      year: article.year,
      doi: article.doi,
      status: nextStatus,
      abstract: article.abstract ?? "",
      inclusionSummary: summary,
    })
        .then((updatedArticle) => {
          setArticles((currentArticles) =>
              currentArticles.map((item) => (item.id === updatedArticle.id ? updatedArticle : item)),
          );
          void loadSurveyData();
          toast.success(decision === "added" ? "Article added to this survey" : "Article marked as not added");
        })
        .catch((err) => toast.error(err instanceof Error ? err.message : "Article update failed"));
  };

  const handleAddToNewSurvey = (referenceId: string) => {
    const newSurveyName = prompt("Enter a name for the new survey:");
    if (!newSurveyName || newSurveyName.trim() === "") return;

    createSurveyWithArticle(newSurveyName, referenceId)
        .then((newSurvey) => {
          toast.success(`Successfully created "${newSurvey.name}"!`);
          navigate(`/survey/${newSurvey.id}`);
        })
        .catch((err) => {
          toast.error(err instanceof Error ? err.message : "Failed to create new survey");
        });
  };

  const handleStatCardClick = (filter: ArticleFilter) => {
    setArticleFilter(filter);
    setActiveTab("articles");
  };

  const handleTaxonomyImport = async () => {
    if (!taxonomyFile || isImportingTaxonomy) return;

    try {
      setIsImportingTaxonomy(true);
      setTaxonomyImportError(null);
      setTaxonomyImportSummary(null);

      const summary = await importTaxonomy(taxonomyFile);
      const importedDimensions = summary.dimensions.map((dimension) => ({
        name: dimension.name,
        options: dimension.options ?? [],
      }));

      setCustomDimensions(importedDimensions);
      localStorage.setItem(`survey:${surveyId}:dimensions`, JSON.stringify(importedDimensions));
      setTaxonomyImportSummary(summary);
      toast.success("Taxonomy imported successfully");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Taxonomy import failed";
      setTaxonomyImportError(message);
      toast.error(message);
    } finally {
      setIsImportingTaxonomy(false);
    }
  };

  const handleClassifiedArticlesImport = async () => {
    if (!classifiedArticlesFile || isImportingClassifiedArticles) return;

    try {
      setIsImportingClassifiedArticles(true);
      setClassifiedArticlesImportError(null);
      setClassifiedArticlesImportSummary(null);

      const summary = await importClassifiedArticles(surveyId, classifiedArticlesFile);
      setClassifiedArticlesImportSummary(summary);
      void loadSurveyData();
      toast.success("Classified articles imported successfully");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Classified article import failed";
      setClassifiedArticlesImportError(message);
      toast.error(message);
    } finally {
      setIsImportingClassifiedArticles(false);
    }
  };

  const handleImport = (type: "url" | "bibtex" | "pdf", data: string | File) => {
    const addedBy = {
      id: currentUser.id,
      name: currentUser.name,
      role: currentUser.role,
    };

    importSurveyArticle(surveyId, {
      type,
      data,
      addedBy,
    })
        .then((newArticle) => {
          setArticles((currentArticles) => [newArticle, ...currentArticles]);
          setIsImportOpen(false);
          void loadSurveyData();
          toast.success("Article imported successfully!");
        })
        .catch((err) => toast.error(err instanceof Error ? err.message : "Article import failed"));
  };

  const handleView = (id: string) => {
    navigate(`/survey/${surveyId}/articles/${id}`);
  };

  const handleOpen = (article: Article) => {
    navigate(`/survey/${surveyId}/articles/${article.id}`);
  };

  const handleEdit = (id: string) => {
    navigate(`/survey/${surveyId}/articles/${id}/edit`);
  };

  const handleDelete = (id: string) => {
    const article = articles.find((item) => item.id === id);
    const canDelete = isOwnerView || article?.addedBy?.id === currentUser.id;
    if (!canDelete) {
      toast.error("Reviewers can only remove articles they added");
      return;
    }

    deleteSurveyArticle(surveyId, id)
        .then(() => {
          setArticles((currentArticles) => currentArticles.filter((a) => a.id !== id));
          void loadSurveyData();
          toast.success("Article deleted");
        })
        .catch((err) => toast.error(err instanceof Error ? err.message : "Article delete failed"));
  };

  const handleUpdateStatus = (id: string, status: ArticleStatus) => {
    const article = articles.find((item) => item.id === id);
    if (!article) {
      toast.error("Article not found");
      return;
    }

    updateSurveyArticle(surveyId, id, {
      title: article.title,
      authors: article.authors,
      journal: article.journal,
      year: article.year,
      doi: article.doi,
      status,
      abstract: article.abstract ?? "",
      inclusionSummary: article.inclusionSummary ?? "",
    })
        .then((updatedArticle) => {
          setArticles((currentArticles) =>
              currentArticles.map((current) => (current.id === id ? updatedArticle : current)),
          );
          toast.success(`Status updated to ${status}`);
        })
        .catch((err) => toast.error(err instanceof Error ? err.message : "Status update failed"));
  };

  const filteredArticles = articles.filter((article) => {
    if (articleFilter === "all") return true;
    if (articleFilter === "screened") return article.status === "INCLUDED" || article.status === "EXCLUDED";
    if (articleFilter === "pending") return article.status === "PENDING";
    return true;
  });

  const tabs = [
    { id: "dashboard" as TabType, label: "Dashboard", icon: LayoutDashboard },
    { id: "articles" as TabType, label: "Articles", icon: FileText },
    { id: "ask-ai" as TabType, label: "Ask AI", icon: Sparkles },
    { id: "reviewers" as TabType, label: "Reviewers", icon: UsersRound },
  ];

  return (
      <div className="min-h-screen bg-gray-50">
        <Toaster position="top-right" />

        {/* Header */}
        <div className="border-b border-gray-200 bg-white">
          <div className="max-w-7xl mx-auto px-6 py-6">
            <button
                onClick={() => navigate("/survey")}
                className="inline-flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-gray-600 hover:text-gray-900 rounded-lg hover:bg-gray-100 transition-colors mb-4"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to Surveys
            </button>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-3xl font-semibold text-gray-900">
                {survey.name}
              </h1>
              <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-700 border border-blue-200">
              {survey.status}
            </span>
            </div>
            <p className="text-sm text-gray-500">
              Created on {survey.createdDate ? new Date(survey.createdDate).toLocaleDateString() : "N/A"}
            </p>
            <div className="mt-4 inline-flex rounded-lg border border-gray-200 bg-gray-50 p-1">
              {!authUser && (
                  <>
                    <button
                        type="button"
                        onClick={() => navigate(`/survey/${surveyId}?role=owner`)}
                        className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                            currentRole === "Owner"
                                ? "bg-white text-blue-700 shadow-sm"
                                : "text-gray-600 hover:text-gray-900"
                        }`}
                    >
                      Owner
                    </button>
                    <button
                        type="button"
                        onClick={() => navigate(`/survey/${surveyId}?role=reviewer`)}
                        className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                            currentRole === "Reviewer"
                                ? "bg-white text-blue-700 shadow-sm"
                                : "text-gray-600 hover:text-gray-900"
                        }`}
                    >
                      Reviewer
                    </button>
                  </>
              )}
            </div>
          </div>

          {/* Tab Navigation */}
          <div className="max-w-7xl mx-auto px-6">
            <div className="flex gap-1 border-b border-gray-200">
              {tabs.map((tab) => {
                const Icon = tab.icon;
                const isActive = activeTab === tab.id;
                return (
                    <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                            isActive
                                ? "border-blue-600 text-blue-600 bg-blue-50/50"
                                : "border-transparent text-gray-600 hover:text-gray-900 hover:bg-gray-50"
                        }`}
                    >
                      <Icon className="w-4 h-4" />
                      {tab.label}
                    </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Content Body */}
        <div className="max-w-7xl mx-auto px-6 py-8">
          {activeTab === "dashboard" && (
              <div className="space-y-6">
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                  <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Description
                  </h2>
                  <p className="text-sm text-gray-700 leading-relaxed mb-4">
                    {survey.description}
                  </p>
                  <div className="pt-4 border-t border-gray-100">
                    <h3 className="text-sm font-semibold text-gray-900 mb-2">
                      Research Question
                    </h3>
                    <p className="text-sm text-gray-700 italic">
                      {survey.researchQuestion}
                    </p>
                  </div>
                </div>

                {/* Structured Extraction Dimensions Configuration Section */}
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                  {/* Header Title Section */}
                  <div
                      className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-100 pb-4 mb-4">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2 bg-blue-50 rounded-lg text-blue-600">
                        <Sliders className="w-5 h-5"/>
                      </div>
                      <div>
                        <h2 className="text-lg font-semibold text-gray-900">Structured Review Criteria</h2>
                        <p className="text-xs text-gray-500">Configure core appraisal metrics and mapped categorical
                          label configurations</p>
                      </div>
                    </div>

                    {isOwnerView && (
                        <div className="flex items-center gap-3">
                          <button
                              type="button"
                              onClick={() => {
                                setCustomDimensions([]);
                                localStorage.setItem(`survey:${surveyId}:dimensions`, JSON.stringify([]));
                              }}
                              className="px-2.5 py-1 text-xs font-medium text-gray-600 hover:bg-gray-100 rounded transition-colors"
                          >
                            Clear All
                          </button>

                          <button
                              type="button"
                              disabled={savingCriteria}
                              onClick={handleSaveCriteriaToDatabase}
                              className="inline-flex items-center gap-1.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs px-3 py-1.5 rounded-lg shadow-sm transition disabled:bg-gray-300 cursor-pointer"
                          >
                            <Save className="w-3.5 h-3.5"/>
                            {savingCriteria ? "Syncing..." : "Save Selection"}
                          </button>
                        </div>
                    )}
                  </div>

                  {/* 🟢 Step-by-Step Interactive Form Builder (Visible to Owner Only) */}
                  {isOwnerView && (
                      <div className="mb-6 rounded-xl border border-blue-100 bg-blue-50/40 p-4">
                        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                          <div className="space-y-2">
                            <div className="flex items-center gap-2">
                              <Upload className="h-4 w-4 text-blue-600" />
                              <h3 className="text-sm font-semibold text-gray-900">Import taxonomy JSON</h3>
                            </div>
                            <p className="text-xs text-gray-600">
                              Upload the unified taxonomy JSON to create or update reusable dimensions and values.
                              Imported dimensions replace the active workspace below.
                            </p>
                            <input
                                type="file"
                                accept="application/json,.json"
                                onChange={(event) => {
                                  setTaxonomyFile(event.target.files?.[0] ?? null);
                                  setTaxonomyImportError(null);
                                  setTaxonomyImportSummary(null);
                                }}
                                className="block w-full text-xs text-gray-700 file:mr-3 file:rounded-md file:border-0 file:bg-white file:px-3 file:py-2 file:text-xs file:font-semibold file:text-blue-700 hover:file:bg-blue-100"
                            />
                          </div>

                          <button
                              type="button"
                              onClick={handleTaxonomyImport}
                              disabled={!taxonomyFile || isImportingTaxonomy}
                              className="inline-flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300"
                          >
                            <Upload className="h-3.5 w-3.5" />
                            {isImportingTaxonomy ? "Importing..." : "Import Taxonomy"}
                          </button>
                        </div>

                        {taxonomyImportError && (
                            <div className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
                              {taxonomyImportError}
                            </div>
                        )}

                        {taxonomyImportSummary && (
                            <div className="mt-3 rounded-lg border border-green-200 bg-white px-3 py-3 text-xs text-gray-700">
                              <p className="font-semibold text-green-700">Import completed.</p>
                              <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3">
                                <span>Dimensions created: {taxonomyImportSummary.dimensionsCreated}</span>
                                <span>Dimensions updated: {taxonomyImportSummary.dimensionsUpdated}</span>
                                <span>Dimensions skipped: {taxonomyImportSummary.dimensionsSkipped}</span>
                                <span>Values created: {taxonomyImportSummary.valuesCreated}</span>
                                <span>Values updated: {taxonomyImportSummary.valuesUpdated}</span>
                                <span>Values skipped: {taxonomyImportSummary.valuesSkipped}</span>
                              </div>
                              {taxonomyImportSummary.warnings.length > 0 && (
                                  <div className="mt-2">
                                    <p className="font-semibold text-amber-700">Warnings</p>
                                    <ul className="mt-1 list-disc pl-5">
                                      {taxonomyImportSummary.warnings.map((warning, index) => (
                                          <li key={`${warning}-${index}`}>{warning}</li>
                                      ))}
                                    </ul>
                                  </div>
                              )}
                            </div>
                        )}

                        <div className="mt-4 border-t border-blue-100 pt-4">
                          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                            <div className="space-y-2">
                              <div className="flex items-center gap-2">
                                <Upload className="h-4 w-4 text-purple-600" />
                                <h3 className="text-sm font-semibold text-gray-900">Import classified articles JSON</h3>
                              </div>
                              <p className="text-xs text-gray-600">
                                Upload classified articles after the taxonomy exists. Unknown taxonomy dimensions or
                                values are skipped and reported as warnings.
                              </p>
                              <input
                                  type="file"
                                  accept="application/json,.json"
                                  onChange={(event) => {
                                    setClassifiedArticlesFile(event.target.files?.[0] ?? null);
                                    setClassifiedArticlesImportError(null);
                                    setClassifiedArticlesImportSummary(null);
                                  }}
                                  className="block w-full text-xs text-gray-700 file:mr-3 file:rounded-md file:border-0 file:bg-white file:px-3 file:py-2 file:text-xs file:font-semibold file:text-purple-700 hover:file:bg-purple-100"
                              />
                            </div>

                            <button
                                type="button"
                                onClick={handleClassifiedArticlesImport}
                                disabled={!classifiedArticlesFile || isImportingClassifiedArticles}
                                className="inline-flex items-center justify-center gap-2 rounded-lg bg-purple-600 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-purple-700 disabled:cursor-not-allowed disabled:bg-gray-300"
                            >
                              <Upload className="h-3.5 w-3.5" />
                              {isImportingClassifiedArticles ? "Importing..." : "Import Articles"}
                            </button>
                          </div>

                          {classifiedArticlesImportError && (
                              <div className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
                                {classifiedArticlesImportError}
                              </div>
                          )}

                          {classifiedArticlesImportSummary && (
                              <div className="mt-3 rounded-lg border border-green-200 bg-white px-3 py-3 text-xs text-gray-700">
                                <p className="font-semibold text-green-700">Classified articles import completed.</p>
                                <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3">
                                  <span>Articles created: {classifiedArticlesImportSummary.articlesCreated}</span>
                                  <span>Articles updated: {classifiedArticlesImportSummary.articlesUpdated}</span>
                                  <span>Articles skipped: {classifiedArticlesImportSummary.articlesSkipped}</span>
                                  <span>Survey links created: {classifiedArticlesImportSummary.surveyLinksCreated}</span>
                                  <span>Survey links skipped: {classifiedArticlesImportSummary.surveyLinksSkipped}</span>
                                  <span>Classifications created: {classifiedArticlesImportSummary.classificationsCreated}</span>
                                  <span>Classifications updated: {classifiedArticlesImportSummary.classificationsUpdated}</span>
                                  <span>Classifications skipped: {classifiedArticlesImportSummary.classificationsSkipped}</span>
                                </div>
                                {classifiedArticlesImportSummary.warnings.length > 0 && (
                                    <div className="mt-2">
                                      <p className="font-semibold text-amber-700">Warnings</p>
                                      <ul className="mt-1 max-h-32 list-disc overflow-auto pl-5">
                                        {classifiedArticlesImportSummary.warnings.map((warning, index) => (
                                            <li key={`${warning}-${index}`}>{warning}</li>
                                        ))}
                                      </ul>
                                    </div>
                                )}
                                {classifiedArticlesImportSummary.errors.length > 0 && (
                                    <div className="mt-2">
                                      <p className="font-semibold text-red-700">Errors</p>
                                      <ul className="mt-1 max-h-32 list-disc overflow-auto pl-5">
                                        {classifiedArticlesImportSummary.errors.map((error, index) => (
                                            <li key={`${error}-${index}`}>{error}</li>
                                        ))}
                                      </ul>
                                    </div>
                                )}
                              </div>
                          )}
                        </div>
                      </div>
                  )}

                  {isOwnerView && (
                      <div
                          className="space-y-5 max-w-2xl bg-white p-5 rounded-xl border border-gray-200 shadow-sm mb-6">
                        {/* Header Info */}
                        <div className="border-b border-gray-100 pb-2">
    <span className="text-xs font-bold uppercase tracking-wider text-blue-600 block">
      Create New Dimension Template
    </span>
                          <p className="text-[11px] text-gray-400">
                            Define a structural appraisal metric and its corresponding categorical labels
                          </p>
                        </div>

                        <div className="space-y-4">
                          {/* Row 1: Primary Dimension Core Identity */}
                          <div className="space-y-1.5">
                            <label className="text-xs font-semibold text-gray-700 flex items-center gap-1.5">
        <span
            className="flex items-center justify-center w-4 h-4 bg-blue-50 text-blue-600 rounded-full text-[10px] font-bold">
          1
        </span>
                              Dimension Name
                            </label>
                            <input
                                type="text"
                                value={dimensionInput}
                                onChange={(e) => setDimensionInput(e.target.value)}
                                placeholder="e.g., Target Dataset Context or Evaluation Metrics Utilized"
                                className="w-full px-3 py-2 text-sm bg-gray-50/50 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all placeholder:text-gray-400"
                            />
                          </div>

                          {/* Row 2: Sub-Options Append Configuration area */}
                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200/60 space-y-3">
                            <label className="text-xs font-semibold text-gray-700 flex items-center gap-1.5">
        <span
            className="flex items-center justify-center w-4 h-4 bg-gray-200 text-gray-600 rounded-full text-[10px] font-bold">
          2
        </span>
                              Define Categorical Option Selection Choices
                            </label>

                            <div className="flex gap-2">
                              <input
                                  type="text"
                                  value={optionInput}
                                  onChange={(e) => setOptionInput(e.target.value)}
                                  onKeyDown={(e) => {
                                    if (e.key === 'Enter') {
                                      e.preventDefault();
                                      handleAddOptionToStaging();
                                    }
                                  }}
                                  placeholder="e.g., Audio/Music"
                                  className="w-full px-3 py-2 text-sm bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all placeholder:text-gray-400"
                              />
                              <button
                                  type="button"
                                  onClick={handleAddOptionToStaging}
                                  className="px-4 bg-gray-900 hover:bg-gray-800 text-white rounded-lg text-xs font-semibold transition-colors shrink-0"
                              >
                                Add Choice
                              </button>
                            </div>

                            {/* Temporary option tags staging layout area */}
                            {currentOptionsBuild.length > 0 ? (
                                <div
                                    className="flex flex-wrap gap-1.5 p-2 bg-white rounded-lg border border-gray-200/80 min-h-[38px] items-center">
                                  {currentOptionsBuild.map(opt => (
                                      <span
                                          key={opt}
                                          className="inline-flex items-center gap-1.5 bg-blue-50/60 border border-blue-100 text-blue-800 px-2.5 py-0.5 rounded-md text-xs font-medium animate-in fade-in-50 duration-150"
                                      >
              {opt}
                                        <button
                                            type="button"
                                            onClick={() => handleRemoveOptionFromStaging(opt)}
                                            className="text-blue-400 hover:text-red-500 font-bold transition-colors ml-0.5 text-sm leading-none"
                                        >
                &times;
              </button>
            </span>
                                  ))}
                                </div>
                            ) : (
                                <p className="text-[11px] text-gray-400 italic pl-1">
                                  No custom selector entries appended yet. Leaving this empty switches input fields into
                                  free text mode.
                                </p>
                            )}
                          </div>
                        </div>

                        {/* Form Action Submissions */}
                        <div className="pt-2 flex justify-end">
                          <button
                              type="button"
                              onClick={handleAddDimension}
                              className="w-full sm:w-auto px-5 py-2 inline-flex items-center justify-center gap-2 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-lg shadow-sm transition-all transform active:scale-[0.98] cursor-pointer"
                          >
                            <Plus className="w-4 h-4"/> Save Entry Template
                          </button>
                        </div>
                      </div>
                  )}

                  {/* 🟢 Workspace Rendering Current Dimensions Grid View */}
                  <div className="space-y-3">
                    <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block">Active Metrics Workspace</span>
                    {customDimensions.length === 0 ? (
                        <div
                            className="border border-dashed border-gray-200 rounded-xl p-6 min-h-[70px] bg-gray-50/50 text-center text-xs text-gray-400 italic">
                          No metrics assigned yet. Design custom structures above to start tracking variables.
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          {customDimensions.map((dim, idx) => (
                              <div key={`${dim.name}-${idx}`}
                                   className="p-4 rounded-xl border border-gray-200 bg-white shadow-xs flex flex-col justify-between space-y-3">
                                <div>
                                  <div className="flex justify-between items-start">
                                    <h4 className="text-sm font-bold text-gray-800">{dim.name}</h4>
                                    {isOwnerView && (
                                        <button
                                            type="button"
                                            onClick={() => handleRemoveDimension(dim.name)}
                                            className="text-gray-400 hover:text-red-600 transition"
                                        >
                                          <X className="w-4 h-4"/>
                                        </button>
                                    )}
                                  </div>

                                  {/* Internal pill indicators displaying the assigned select options maps */}
                                  <div className="flex flex-wrap gap-1.5 mt-2">
                                    {dim.options.length === 0 ? (
                                        <span className="text-[11px] text-gray-400 italic">Free text input fallback tag fields mode</span>
                                    ) : (
                                        dim.options.map(opt => (
                                            <span key={opt}
                                                  className="bg-blue-50 text-blue-700 border border-blue-100 text-[10px] px-2 py-0.5 rounded-md font-medium">
                              {opt}
                            </span>
                                        ))
                                    )}
                                  </div>
                                </div>
                              </div>
                          ))}
                        </div>
                    )}
                  </div>
                </div>

                {/* Clickable Statistics Cards */}
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-semibold text-gray-900">
                      Statistics Overview
                    </h2>
                    <p className="text-xs text-gray-500">
                      Click any card to view filtered articles
                    </p>
                  </div>
                  <div className="grid grid-cols-3 gap-4">
                    <button
                        onClick={() => handleStatCardClick("all")}
                        className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 hover:shadow-md hover:border-blue-300 transition-all group cursor-pointer text-left"
                    >
                      <div className="flex items-start justify-between mb-3">
                        <div
                            className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-200 transition-colors">
                          <FileText className="w-6 h-6 text-blue-600"/>
                        </div>
                        <ArrowRight
                            className="w-5 h-5 text-gray-400 group-hover:text-blue-600 group-hover:translate-x-1 transition-all"/>
                      </div>
                      <p className="text-3xl font-semibold text-gray-900 mb-1">
                        {totalArticles}
                      </p>
                      <p className="text-sm text-gray-600">Total Articles</p>
                      <p className="text-xs text-gray-500 mt-2">View all articles →</p>
                    </button>

                    <button
                        onClick={() => handleStatCardClick("screened")}
                        className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 hover:shadow-md hover:border-green-300 transition-all group cursor-pointer text-left"
                    >
                      <div className="flex items-start justify-between mb-3">
                        <div
                            className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center group-hover:bg-green-200 transition-colors">
                          <Users className="w-6 h-6 text-green-600"/>
                        </div>
                        <ArrowRight
                            className="w-5 h-5 text-gray-400 group-hover:text-green-600 group-hover:translate-x-1 transition-all"/>
                      </div>
                      <p className="text-3xl font-semibold text-gray-900 mb-1">
                        {screenedArticles}
                      </p>
                      <p className="text-sm text-gray-600">Screened Articles</p>
                      <p className="text-xs text-gray-500 mt-2">Included & excluded →</p>
                    </button>

                    <button
                        onClick={() => handleStatCardClick("pending")}
                        className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 hover:shadow-md hover:border-yellow-300 transition-all group cursor-pointer text-left"
                    >
                      <div className="flex items-start justify-between mb-3">
                        <div
                            className="w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center group-hover:bg-yellow-200 transition-colors">
                          <Calendar className="w-6 h-6 text-yellow-600"/>
                        </div>
                        <ArrowRight
                            className="w-5 h-5 text-gray-400 group-hover:text-yellow-600 group-hover:translate-x-1 transition-all"/>
                      </div>
                      <p className="text-3xl font-semibold text-gray-900 mb-1">
                        {pendingArticles}
                      </p>
                      <p className="text-sm text-gray-600">Pending Review</p>
                      <p className="text-xs text-gray-500 mt-2">Needs screening →</p>
                    </button>
                  </div>
                </div>

                <ContributorsPanel
                    contributors={contributors}
                    onAddContributor={handleAddContributor}
                    onRemoveContributor={handleRemoveContributor}
                    canManageReviewers={isOwnerView}
                />

                {/* Quick Actions */}
                <div className="bg-gradient-to-br from-blue-50 to-purple-50 rounded-xl border border-gray-200 p-6">
                  <h2 className="text-lg font-semibold text-gray-900 mb-4">
                    Quick Actions
                  </h2>
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                    <button
                        onClick={() => setActiveTab("articles")}
                        className="bg-white rounded-lg p-4 text-left hover:shadow-md transition-all group border border-gray-200"
                    >
                      <FileText className="w-5 h-5 text-blue-600 mb-2"/>
                      <p className="text-sm font-medium text-gray-900">Manage Articles</p>
                      <p className="text-xs text-gray-500">Import and review papers</p>
                    </button>
                    <button
                        onClick={() => setActiveTab("ask-ai")}
                        className="bg-white rounded-lg p-4 text-left hover:shadow-md transition-all group border border-gray-200"
                    >
                      <Sparkles className="w-5 h-5 text-purple-600 mb-2"/>
                      <p className="text-sm font-medium text-gray-900">Ask AI</p>
                      <p className="text-xs text-gray-500">Get insights from your research</p>
                    </button>
                    <button
                        onClick={() => setActiveTab("reviewers")}
                        className="bg-white rounded-lg p-4 text-left hover:shadow-md transition-all group border border-gray-200"
                    >
                      <UsersRound className="w-5 h-5 text-green-600 mb-2"/>
                      <p className="text-sm font-medium text-gray-900">Add Reviewers</p>
                      <p className="text-xs text-gray-500">Assign reviewers to this survey</p>
                    </button>
                  </div>
                </div>
              </div>
          )}

          {/* Reviewers Tab */}
          {activeTab === "reviewers" && (
              <ContributorsPanel
                  contributors={contributors}
                  onAddContributor={handleAddContributor}
                  onRemoveContributor={handleRemoveContributor}
                  canManageReviewers={isOwnerView}
              />
          )}

          {/* Articles Tab */}
          {activeTab === "articles" && (
              <div className="space-y-6">
                {articleFilter !== "all" && (
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Filter className="w-4 h-4 text-blue-600"/>
                        <p className="text-sm font-medium text-blue-900">
                          Showing: {articleFilter === "screened" ? "Screened Articles (Included & Excluded)" : "Pending Review"}
                        </p>
                      </div>
                      <button
                          onClick={() => setArticleFilter("all")}
                          className="text-sm text-blue-600 hover:text-blue-700 font-medium"
                      >
                        Clear filter
                      </button>
                    </div>
                )}

                {isImportOpen && (
                    <ImportSection
                        onImport={handleImport}
                        isOpen={isImportOpen}
                        onClose={() => setIsImportOpen(false)}
                    />
                )}

                {!isImportOpen && (
                    <button
                        onClick={() => setIsImportOpen(true)}
                        className="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white rounded-xl p-4 flex items-center justify-center gap-2 font-medium transition-all shadow-sm"
                    >
                      <FileText className="w-5 h-5"/>
                      Import New Articles
                    </button>
                )}

                {filteredArticles.length === 0 ? (
                    <EmptyState onAddArticle={() => setIsImportOpen(true)}/>
                ) : (
                    <div>
                      <div className="flex items-center justify-between mb-6">
                        <p className="text-sm text-gray-600">
                          {filteredArticles.length} article{filteredArticles.length !== 1 ? "s" : ""}
                        </p>
                      </div>
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {filteredArticles.map((article) => (
                            <ArticleCard
                                key={article.id}
                                article={article}
                                onView={handleView}
                                onOpen={handleOpen}
                                onEdit={handleEdit}
                                onDelete={handleDelete}
                                onUpdateStatus={handleUpdateStatus}
                                canEdit={isOwnerView || article.addedBy?.id === currentUser.id}
                                canDelete={isOwnerView || article.addedBy?.id === currentUser.id}
                            />
                        ))}
                      </div>
                    </div>
                )}
              </div>
          )}

          {activeTab === "ask-ai" && (
              <div className="space-y-6">
                <AskInput onAsk={handleAsk} isLoading={isLoading}/>

                {isLoading && <LoadingState/>}

                {showResults && currentQuestion && currentAnswer && (
                    <>
                      <AIAnswer
                          question={currentQuestion}
                          answer={currentAnswer}
                          sourceCount={topReferences.length}
                          confidence={85}
                      />
                      {topReferences.length > 0 ? (
                          <ReferencesList
                              references={topReferences}
                              surveyId={surveyId}
                              decisions={referenceDecisions}
                              onReviewReference={handleReferenceDecision}
                              onAddToNewSurvey={handleAddToNewSurvey}
                          />
                      ) : (
                          <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                            <h3 className="text-sm font-semibold text-gray-900 mb-2">Top References</h3>
                            <p className="text-sm text-gray-600">
                              No survey database references are available yet. Import articles into this survey to
                              populate this list. The answer above is based on model knowledge only.
                            </p>
                          </div>
                      )}
                    </>
                )}

                {!isLoading && !showResults && <EmptyAskState articleCount={totalArticles} />}
              </div>
          )}
        </div>
      </div>
  );
}

function isReviewerFallback(roleParam: string | null) {
  return roleParam === "reviewer";
}
