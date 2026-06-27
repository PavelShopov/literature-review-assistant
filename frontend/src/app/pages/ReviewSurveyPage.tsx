import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeft, CheckCircle, FileText, XCircle, Check, AlertCircle } from "lucide-react";
import { toast, Toaster } from "sonner";
import { getArticlesToReview, submitArticleReview, type ReviewableArticle } from "../api/client";

export default function ReviewSurveyPage() {
  const { surveyId } = useParams<{ surveyId: string }>();
  const navigate = useNavigate();
  const [articles, setArticles] = useState<ReviewableArticle[]>([]);
  const [loading, setLoading] = useState(true);

  // Track which article the user is currently looking at
  const [selectedArticleId, setSelectedArticleId] = useState<string | null>(null);

  // States for the active review note modal
  const [activeArticle, setActiveArticle] = useState<ReviewableArticle | null>(null);
  const [note, setNote] = useState("");
  const [decision, setDecision] = useState<"approve" | "deny" | null>(null);

  useEffect(() => {
    if (surveyId) {
      getArticlesToReview(surveyId)
          .then((data) => {
            setArticles(data);
            if (data.length > 0) {
              // Default select the first article
              setSelectedArticleId(data[0].externalId);
            }
          })
          .catch((err) => {
            console.error(err);
            toast.error("Failed to load articles for review");
          })
          .finally(() => setLoading(false));
    }
  }, [surveyId]);

  // Calculate review metrics
  const totalArticles = articles.length;
  const reviewedCount = articles.filter((a) => a.reviewed).length;
  const progressPercentage = totalArticles > 0 ? (reviewedCount / totalArticles) * 100 : 0;

  // Find the currently selected article object
  const currentArticle = articles.find((a) => a.externalId === selectedArticleId);

  const handleOpenReview = (article: ReviewableArticle, type: "approve" | "deny") => {
    setActiveArticle(article);
    setDecision(type);

    let initialNote = "";
    if (article.jsonResponse) {
      try {
        const parsed = JSON.parse(article.jsonResponse);
        if (parsed.note) initialNote = parsed.note;
        if (parsed.decision) setDecision(parsed.decision);
      } catch (e) {
        initialNote = article.jsonResponse;
      }
    }
    setNote(initialNote);
  };

  const handleSubmitReview = async () => {
    if (!activeArticle || !decision || !surveyId) return;

    const payload = JSON.stringify({
      decision,
      note,
      timestamp: new Date().toISOString()
    });

    try {
      await submitArticleReview(surveyId, activeArticle.externalId, payload);
      toast.success(`Successfully submitted review for ${activeArticle.title}`);

      // Update local array state
      setArticles(articles.map(a =>
          a.externalId === activeArticle.externalId
              ? { ...a, reviewed: true, jsonResponse: payload }
              : a
      ));

      setActiveArticle(null);
      setNote("");
    } catch (err) {
      toast.error("Failed to submit review");
      console.error(err);
    }
  };

  // Helper to check decision type inside saving string
  const getDecisionStatus = (article: ReviewableArticle) => {
    if (!article.reviewed || !article.jsonResponse) return null;
    try {
      const parsed = JSON.parse(article.jsonResponse);
      return parsed.decision as "approve" | "deny";
    } catch {
      return null;
    }
  };

  if (loading) {
    return <div className="p-8 text-center text-gray-500">Loading articles...</div>;
  }

  return (
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <Toaster position="top-right" />

        {/* Header */}
        <div className="border-b border-gray-200 bg-white">
          <div className="max-w-7xl mx-auto px-6 py-6">
            <button
                onClick={() => navigate(-1)}
                className="mb-3 inline-flex items-center gap-2 text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to Surveys To Review
            </button>

            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
              <div>
                <h1 className="text-2xl font-semibold text-gray-900">Review Articles Dashboard</h1>
                <p className="text-sm text-gray-500 mt-0.5">
                  Go through each article below to review and cast your decision.
                </p>
              </div>

              {/* Progress Visualizer */}
              <div className="bg-gray-100 p-4 rounded-xl border border-gray-200 min-w-[240px]">
                <div className="flex justify-between items-center mb-1.5 text-sm font-medium text-gray-700">
                  <span>Review Progress</span>
                  <span className="font-bold text-blue-600">{reviewedCount} / {totalArticles} Articles</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                      className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${progressPercentage}%` }}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Main Split Interface */}
        <div className="max-w-7xl w-full mx-auto px-6 py-8 flex-1 grid grid-cols-1 lg:grid-cols-12 gap-8">

          {/* Left Side: Article Selector Navigation list */}
          <div className="lg:col-span-4 space-y-2">
            <h2 className="text-xs font-bold text-gray-400 uppercase tracking-wider px-1 mb-3">
              Survey Article List
            </h2>
            {articles.length === 0 ? (
                <p className="text-sm text-gray-500 p-4 bg-white rounded-xl border border-gray-200">No items available.</p>
            ) : (
                <div className="space-y-2 max-h-[calc(100vh-280px)] overflow-y-auto pr-1">
                  {articles.map((article, idx) => {
                    const isSelected = article.externalId === selectedArticleId;
                    const articleDecision = getDecisionStatus(article);

                    return (
                        <button
                            key={article.externalId}
                            onClick={() => setSelectedArticleId(article.externalId)}
                            className={`w-full text-left p-4 rounded-xl border transition-all flex items-start justify-between gap-3 ${
                                isSelected
                                    ? "bg-blue-50 border-blue-400 shadow-sm"
                                    : "bg-white border-gray-200 hover:border-gray-300"
                            }`}
                        >
                          <div className="flex-1 min-w-0">
                            <p className="text-xs font-semibold text-gray-400 mb-1">Article #{idx + 1}</p>
                            <h4 className={`text-sm font-semibold truncate ${isSelected ? "text-blue-900" : "text-gray-900"}`}>
                              {article.title}
                            </h4>
                            <p className="text-xs text-gray-500 truncate mt-0.5">{article.journal || "Unknown Journal"}</p>
                          </div>

                          {/* Tiny Status Indicator Badge */}
                          {article.reviewed ? (
                              articleDecision === "approve" ? (
                                  <span className="p-1 rounded-full bg-green-100 text-green-700" title="Approved">
                          <Check className="w-3.5 h-3.5 stroke-[3]" />
                        </span>
                              ) : (
                                  <span className="p-1 rounded-full bg-red-100 text-red-700" title="Denied">
                          <XCircle className="w-3.5 h-3.5" />
                        </span>
                              )
                          ) : (
                              <span className="p-1 rounded-full bg-amber-100 text-amber-700" title="Pending Review">
                        <AlertCircle className="w-3.5 h-3.5" />
                      </span>
                          )}
                        </button>
                    );
                  })}
                </div>
            )}
          </div>

          {/* Right Side: Active Workspace Pane */}
          <div className="lg:col-span-8">
            {currentArticle ? (
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 sticky top-6">
                  <div className="flex flex-col sm:flex-row justify-between items-start gap-4 mb-6 pb-6 border-b border-gray-100">
                    <div>
                      <h3 className="text-xl font-bold text-gray-900 leading-tight">{currentArticle.title}</h3>
                      <p className="text-sm text-gray-500 mt-2">
                        <span className="font-medium text-gray-700">{currentArticle.authors.join(", ")}</span>
                        {currentArticle.journal ? ` — ${currentArticle.journal}` : ""} ({currentArticle.publicationYear})
                      </p>
                    </div>
                    {currentArticle.reviewed && (
                        <div className="flex flex-col items-end shrink-0">
                    <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold ${
                        getDecisionStatus(currentArticle) === "approve" ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"
                    }`}>
                      Reviewed ({getDecisionStatus(currentArticle)})
                    </span>
                          {currentArticle.reviewedBy && (
                              <p className="mt-1 text-xs text-gray-400">by {currentArticle.reviewedBy}</p>
                          )}
                        </div>
                    )}
                  </div>

                  {/* Abstract */}
                  <div className="mb-6">
                    <h4 className="text-sm font-semibold text-gray-900 mb-2 flex items-center gap-1.5">
                      <FileText className="w-4 h-4 text-gray-400" />
                      Abstract
                    </h4>
                    <p className="text-sm text-gray-700 whitespace-pre-line bg-gray-50 p-4 rounded-lg border border-gray-100 max-h-60 overflow-y-auto">
                      {currentArticle.articleAbstract || "No abstract available."}
                    </p>
                  </div>

                  {/* AI Section */}
                  {currentArticle.aiAnnotations && currentArticle.aiAnnotations.length > 0 && (
                      <div className="mb-6">
                        <h4 className="text-sm font-semibold text-blue-900 mb-2 flex items-center gap-2">
                          <span className="w-4 h-4 bg-blue-100 text-blue-600 rounded flex items-center justify-center text-xs font-bold">AI</span>
                          AI-Generated Queries & Extraction
                        </h4>
                        <div className="space-y-3 max-h-64 overflow-y-auto pr-1">
                          {currentArticle.aiAnnotations.map((annotation, idx) => {
                            let parsedData = null;
                            try { parsedData = JSON.parse(annotation.jsonResponse); } catch(e) { /* ignore */ }
                            return (
                                <div key={idx} className="bg-blue-50/50 p-4 rounded-lg border border-blue-100">
                                  <p className="text-xs font-semibold text-blue-800 mb-1.5">Prompt: {annotation.promptText}</p>
                                  {parsedData ? (
                                      <pre className="text-xs text-gray-700 bg-white p-3 rounded border border-gray-200 overflow-auto whitespace-pre-wrap font-mono">
                              {JSON.stringify(parsedData, null, 2)}
                            </pre>
                                  ) : (
                                      <p className="text-xs text-gray-700 bg-white p-3 rounded border border-gray-200 whitespace-pre-wrap">
                                        {annotation.jsonResponse}
                                      </p>
                                  )}
                                </div>
                            );
                          })}
                        </div>
                      </div>
                  )}

                  {/* Actions */}
                  <div className="flex gap-3 pt-2 border-t border-gray-100">
                    <button
                        type="button"
                        onClick={() => handleOpenReview(currentArticle, "approve")}
                        className="inline-flex items-center gap-2 px-5 py-2.5 bg-green-600 hover:bg-green-700 text-white rounded-lg font-medium shadow-sm transition-colors"
                    >
                      <CheckCircle className="w-4 h-4" />
                      Approve Article
                    </button>
                    <button
                        type="button"
                        onClick={() => handleOpenReview(currentArticle, "deny")}
                        className="inline-flex items-center gap-2 px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium shadow-sm transition-colors"
                    >
                      <XCircle className="w-4 h-4" />
                      Deny Article
                    </button>
                  </div>
                </div>
            ) : (
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-12 text-center text-gray-500">
                  Select an article from the left sidebar to begin validation.
                </div>
            )}
          </div>
        </div>

        {/* Review Dialog Modal */}
        {activeArticle && (
            <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
              <div className="bg-white rounded-xl shadow-xl max-w-lg w-full p-6">
                <h3 className="text-lg font-bold text-gray-900 mb-2">
                  {decision === "approve" ? "Approve Article" : "Deny Article"}
                </h3>
                <p className="text-sm text-gray-600 mb-6 line-clamp-2">{activeArticle.title}</p>

                <div className="mb-6">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Review Notes (saved as JSON response)
                  </label>
                  <textarea
                      value={note}
                      onChange={(e) => setNote(e.target.value)}
                      className="w-full border border-gray-300 rounded-lg p-3 h-32 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="Enter your review notes here..."
                  />
                </div>

                <div className="flex justify-end gap-3">
                  <button
                      type="button"
                      onClick={() => setActiveArticle(null)}
                      className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                      type="button"
                      onClick={handleSubmitReview}
                      className={`px-4 py-2 text-sm font-medium text-white rounded-lg transition-colors shadow-sm ${
                          decision === "approve" ? "bg-green-600 hover:bg-green-700" : "bg-red-600 hover:bg-red-700"
                      }`}
                  >
                    Submit Review
                  </button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}