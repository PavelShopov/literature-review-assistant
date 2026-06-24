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
  UsersRound
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
  mockArticles,
  removeContributor,
  updateSurveyArticle,
  type SurveyDetails,
  createSurveyWithArticle,
} from "../api/client";

type TabType = "dashboard" | "articles" | "ask-ai" | "reviewers";
type ArticleFilter = "all" | "screened" | "pending";
type ReferenceDecision = {
  decision: "added" | "skipped";
  summary: string;
  decidedAt: string;
  reviewerName?: string;
};

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

  // Robust owner matching context using backend DTO properties safely
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

    // Fixed: Key mapped directly to abstractText to align with backend DTO models
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
    navigate(`/survey/${surveyId}/articles/${id}/view`);
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

        {/* Content */}
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
                        <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-200 transition-colors">
                          <FileText className="w-6 h-6 text-blue-600" />
                        </div>
                        <ArrowRight className="w-5 h-5 text-gray-400 group-hover:text-blue-600 group-hover:translate-x-1 transition-all" />
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
                        <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center group-hover:bg-green-200 transition-colors">
                          <Users className="w-6 h-6 text-green-600" />
                        </div>
                        <ArrowRight className="w-5 h-5 text-gray-400 group-hover:text-green-600 group-hover:translate-x-1 transition-all" />
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
                        <div className="w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center group-hover:bg-yellow-200 transition-colors">
                          <Calendar className="w-6 h-6 text-yellow-600" />
                        </div>
                        <ArrowRight className="w-5 h-5 text-gray-400 group-hover:text-yellow-600 group-hover:translate-x-1 transition-all" />
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

                <div className="bg-gradient-to-br from-blue-50 to-purple-50 rounded-xl border border-gray-200 p-6">
                  <h2 className="text-lg font-semibold text-gray-900 mb-4">
                    Quick Actions
                  </h2>
                  <div className="grid grid-cols-2 gap-3">
                    <button
                        onClick={() => setActiveTab("articles")}
                        className="bg-white rounded-lg p-4 text-left hover:shadow-md transition-all group border border-gray-200"
                    >
                      <FileText className="w-5 h-5 text-blue-600 mb-2" />
                      <p className="text-sm font-medium text-gray-900">Manage Articles</p>
                      <p className="text-xs text-gray-500">Import and review papers</p>
                    </button>
                    <button
                        onClick={() => setActiveTab("ask-ai")}
                        className="bg-white rounded-lg p-4 text-left hover:shadow-md transition-all group border border-gray-200"
                    >
                      <Sparkles className="w-5 h-5 text-purple-600 mb-2" />
                      <p className="text-sm font-medium text-gray-900">Ask AI</p>
                      <p className="text-xs text-gray-500">Get insights from your research</p>
                    </button>
                  </div>
                </div>
              </div>
          )}

          {activeTab === "reviewers" && (
              <ContributorsPanel
                  contributors={contributors}
                  onAddContributor={handleAddContributor}
                  onRemoveContributor={handleRemoveContributor}
                  canManageReviewers={isOwnerView}
              />
          )}

          {activeTab === "articles" && (
              <div className="space-y-6">
                {articleFilter !== "all" && (
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Filter className="w-4 h-4 text-blue-600" />
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
                      <FileText className="w-5 h-5" />
                      Import New Articles
                    </button>
                )}

                {filteredArticles.length === 0 ? (
                    <EmptyState onAddArticle={() => setIsImportOpen(true)} />
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
                                onEdit={handleEdit}
                                onDelete={handleDelete}
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
                <AskInput onAsk={handleAsk} isLoading={isLoading} />

                {isLoading && <LoadingState />}

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

                {!isLoading && !showResults && (
                    <EmptyAskState articleCount={totalArticles} />
                )}
              </div>
          )}
        </div>
      </div>
  );
}

function isReviewerFallback(roleParam: string | null) {
  return roleParam === "reviewer";
}