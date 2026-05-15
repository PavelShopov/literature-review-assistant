// import { useState } from "react";
import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router";
import { 
  FileText, 
  Users, 
  Calendar, 
  ArrowLeft, 
  LayoutDashboard,
  Sparkles,
  ArrowRight,
  Filter
} from "lucide-react";
import { toast } from "sonner";
import { Toaster } from "sonner";
import { AskInput } from "../components/AskInput";
import { LoadingState } from "../components/LoadingState";
import { AIAnswer } from "../components/AIAnswer";
import { ReferencesList, type Reference } from "../components/ReferencesList";
import { EmptyAskState } from "../components/EmptyAskState";
import { ArticleCard, type Article, type ArticleStatus } from "../components/ArticleCard";
import { ImportSection } from "../components/ImportSection";
import { EmptyState } from "../components/EmptyState";

// Mock articles data
const mockArticles: Article[] = [
  {
    id: "1",
    title: "Deep Learning Applications in Medical Image Analysis: A Systematic Review",
    authors: ["Smith, J.", "Johnson, A.", "Williams, B."],
    journal: "Journal of Medical Imaging",
    year: 2024,
    doi: "10.1000/jmi.2024.001",
    status: "INCLUDED",
    abstract: "This systematic review examines the current state of deep learning applications in medical image analysis.",
  },
  {
    id: "2",
    title: "Machine Learning in Healthcare: Opportunities and Challenges",
    authors: ["Chen, L.", "Rodriguez, M."],
    journal: "Nature Medicine",
    year: 2023,
    doi: "10.1038/nm.2023.456",
    status: "PENDING",
    abstract: "We present a comprehensive analysis of machine learning applications in healthcare settings.",
  },
  {
    id: "3",
    title: "Ethical Considerations in AI-Driven Clinical Decision Support Systems",
    authors: ["Kumar, R.", "Thompson, E.", "Lee, S.", "Davis, K."],
    journal: "The Lancet Digital Health",
    year: 2024,
    doi: "10.1016/s2589-7500(24)00012-3",
    status: "INCLUDED",
    abstract: "This paper explores the ethical implications of deploying AI-driven clinical decision support systems.",
  },
  {
    id: "4",
    title: "Predictive Analytics for Patient Readmission: A Meta-Analysis",
    authors: ["Garcia, M.", "Anderson, P."],
    journal: "JAMA Network Open",
    year: 2023,
    doi: "10.1001/jamanetworkopen.2023.789",
    status: "EXCLUDED",
    abstract: "We conducted a meta-analysis of 85 studies examining the effectiveness of predictive analytics models.",
  },
  {
    id: "5",
    title: "Natural Language Processing in Electronic Health Records: Current State and Future Directions",
    authors: ["Wang, H.", "Brown, T.", "Miller, J."],
    journal: "Journal of Biomedical Informatics",
    year: 2024,
    doi: "10.1016/j.jbi.2024.104321",
    status: "PENDING",
    abstract: "This review article synthesizes recent advances in natural language processing techniques.",
  },
  {
    id: "6",
    title: "AI-Powered Drug Discovery: Accelerating Pharmaceutical Development",
    authors: ["Patel, N.", "Kim, Y.", "O'Brien, M."],
    journal: "Drug Discovery Today",
    year: 2023,
    doi: "10.1016/j.drudis.2023.103567",
    status: "INCLUDED",
    abstract: "We examine how artificial intelligence is revolutionizing the drug discovery pipeline.",
  },
];

// Mock references data
const mockReferences: Reference[] = [
  {
    id: "1",
    title: "Deep Learning Applications in Medical Image Analysis: A Systematic Review",
    authors: ["Smith, J.", "Johnson, A.", "Williams, B."],
    journal: "Journal of Medical Imaging",
    year: 2024,
    snippet: "Our findings demonstrate that deep learning models achieved 94% accuracy in detecting lung nodules, significantly outperforming traditional computer-aided detection systems.",
    relevanceScore: 95,
  },
  {
    id: "3",
    title: "Ethical Considerations in AI-Driven Clinical Decision Support Systems",
    authors: ["Kumar, R.", "Thompson, E.", "Lee, S.", "Davis, K."],
    journal: "The Lancet Digital Health",
    year: 2024,
    snippet: "The deployment of AI in clinical settings raises critical concerns about algorithmic bias and patient autonomy.",
    relevanceScore: 88,
  },
  {
    id: "2",
    title: "Machine Learning in Healthcare: Opportunities and Challenges",
    authors: ["Chen, L.", "Rodriguez, M."],
    journal: "Nature Medicine",
    year: 2023,
    snippet: "Machine learning algorithms have transformed diagnostic processes, reducing average diagnosis time by 40%.",
    relevanceScore: 82,
  },
];

type TabType = "dashboard" | "articles" | "ask-ai";
type ArticleFilter = "all" | "screened" | "pending";
type SurveyDetails = {
  id: string;
  name: string;
  description: string;
  researchQuestion: string;
  createdDate: string;
  status: string;
  totalArticles: number;
  screened: number;
  pending: number;
};
export default function SurveyDetailsPage() {
  const navigate = useNavigate();
  const { surveyId = "survey-001" } = useParams();
  
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

  // Mock survey data
  // const survey = {
  //   id: surveyId,
  //   name: "Impact of AI on Healthcare Outcomes",
  //   description: "A systematic literature review examining the effectiveness and implementation of artificial intelligence technologies in healthcare settings.",
  //   researchQuestion: "How effective are AI technologies in improving patient outcomes and clinical decision-making in healthcare environments?",
  //   createdDate: "2024-03-15",
  //   status: "In Progress",
  //   totalArticles: 6,
  //   screened: 4,
  //   pending: 2,
  // };
  const [survey, setSurvey] = useState<SurveyDetails | null>(null);

  useEffect(() => {
    fetch(`http://localhost:8080/api/surveys/${surveyId}`)
        .then((res) => res.json())
        .then((data) => setSurvey(data))
        .catch((err) => console.error("Survey details fetch error:", err));
  }, [surveyId]);

  if (!survey) {
    return <div>Loading survey...</div>;
  }

  const handleAsk = async (question: string) => {
    setCurrentQuestion(question);
    setIsLoading(true);
    setShowResults(false);

    await new Promise((resolve) => setTimeout(resolve, 3000));

    const mockAnswer = `Based on the analysis of your uploaded research articles, AI technologies have demonstrated significant effectiveness in healthcare settings. 

Key findings include:

1. **Diagnostic Accuracy**: Deep learning models have achieved 94% accuracy in medical image analysis, particularly excelling in early detection of lung nodules and other abnormalities.

2. **Efficiency Gains**: Machine learning algorithms have reduced average diagnosis time by 40% while maintaining high accuracy rates.

3. **Drug Discovery**: AI-powered approaches have dramatically accelerated pharmaceutical development, reducing timelines from 10-15 years to 3-5 years.

4. **Ethical Considerations**: Implementation must address critical concerns including algorithmic bias, patient autonomy, and the need for human oversight.

5. **Clinical Integration**: NLP technologies have enabled automated extraction of clinical information, achieving 92% accuracy in clinical entity recognition.`;

    setCurrentAnswer(mockAnswer);
    setIsLoading(false);
    setShowResults(true);
  };

  const handleAddToSurvey = (referenceId: string, surveyId: string) => {
    toast.success("Article added to current survey");
  };

  const handleAddToNewSurvey = (referenceId: string) => {
    toast.success("Article added to new survey");
  };

  const handleStatCardClick = (filter: ArticleFilter) => {
    setArticleFilter(filter);
    setActiveTab("articles");
  };

  const handleImport = (type: "url" | "bibtex" | "pdf", data: string | File) => {
    const newArticle: Article = {
      id: Date.now().toString(),
      title: `Imported Article via ${type.toUpperCase()}`,
      authors: ["Author, A.", "Researcher, B."],
      journal: "Imported Journal",
      year: 2024,
      doi: "10.1000/imported." + Date.now(),
      status: "PENDING",
      abstract: "This article was imported and needs to be reviewed.",
    };
    setArticles([newArticle, ...articles]);
    setIsImportOpen(false);
    toast.success("Article imported successfully!");
  };

  const handleView = (id: string) => {
    navigate(`/survey/${surveyId}/articles/${id}/view`);
  };

  const handleEdit = (id: string) => {
    navigate(`/survey/${surveyId}/articles/${id}/edit`);
  };

  const handleDelete = (id: string) => {
    setArticles(articles.filter((a) => a.id !== id));
    toast.success("Article deleted");
  };

  const handleUpdateStatus = (id: string, status: ArticleStatus) => {
    setArticles(articles.map((a) => (a.id === id ? { ...a, status } : a)));
    toast.success(`Status updated to ${status}`);
  };

  // Filter articles based on current filter
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
            Created on {new Date(survey.createdDate).toLocaleDateString()}
          </p>
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
        {/* Dashboard Tab */}
        {activeTab === "dashboard" && (
          <div className="space-y-6">
            {/* Description */}
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
                {/* Total Articles */}
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
                    {survey.totalArticles}
                  </p>
                  <p className="text-sm text-gray-600">Total Articles</p>
                  <p className="text-xs text-gray-500 mt-2">
                    View all articles →
                  </p>
                </button>

                {/* Screened */}
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
                    {survey.screened}
                  </p>
                  <p className="text-sm text-gray-600">Screened Articles</p>
                  <p className="text-xs text-gray-500 mt-2">
                    Included & excluded →
                  </p>
                </button>

                {/* Pending */}
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
                    {survey.pending}
                  </p>
                  <p className="text-sm text-gray-600">Pending Review</p>
                  <p className="text-xs text-gray-500 mt-2">
                    Needs screening →
                  </p>
                </button>
              </div>
            </div>

            {/* Quick Actions */}
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

        {/* Articles Tab */}
        {activeTab === "articles" && (
          <div className="space-y-6">
            {/* Filter Info */}
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

            {/* Import Section */}
            {isImportOpen && (
              <ImportSection
                onImport={handleImport}
                isOpen={isImportOpen}
                onClose={() => setIsImportOpen(false)}
              />
            )}

            {/* Add Article Button */}
            {!isImportOpen && (
              <button
                onClick={() => setIsImportOpen(true)}
                className="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white rounded-xl p-4 flex items-center justify-center gap-2 font-medium transition-all shadow-sm"
              >
                <FileText className="w-5 h-5" />
                Import New Articles
              </button>
            )}

            {/* Articles Grid */}
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
                    />
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Ask AI Tab */}
        {activeTab === "ask-ai" && (
          <div className="space-y-6">
            <AskInput onAsk={handleAsk} isLoading={isLoading} />

            {isLoading && <LoadingState />}

            {showResults && currentQuestion && currentAnswer && (
              <>
                <AIAnswer
                  question={currentQuestion}
                  answer={currentAnswer}
                  sourceCount={mockReferences.length}
                  confidence={85}
                />
                <ReferencesList
                  references={mockReferences}
                  surveyId={surveyId}
                  onAddToSurvey={handleAddToSurvey}
                  onAddToNewSurvey={handleAddToNewSurvey}
                />
              </>
            )}

            {!isLoading && !showResults && (
              <EmptyAskState articleCount={survey.totalArticles} />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
