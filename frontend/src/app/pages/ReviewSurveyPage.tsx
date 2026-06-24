import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeft, CheckCircle, FileText, XCircle } from "lucide-react";
import { toast, Toaster } from "sonner";
import { getArticlesToReview, submitArticleReview, type ReviewableArticle } from "../api/client";

export default function ReviewSurveyPage() {
  const { surveyId } = useParams<{ surveyId: string }>();
  const navigate = useNavigate();
  const [articles, setArticles] = useState<ReviewableArticle[]>([]);
  const [loading, setLoading] = useState(true);

  // States for the active review note modal
  const [activeArticle, setActiveArticle] = useState<ReviewableArticle | null>(null);
  const [note, setNote] = useState("");
  const [decision, setDecision] = useState<"approve" | "deny" | null>(null);

  useEffect(() => {
    if (surveyId) {
      getArticlesToReview(surveyId)
        .then(setArticles)
        .catch((err) => {
          console.error(err);
          toast.error("Failed to load articles for review");
        })
        .finally(() => setLoading(false));
    }
  }, [surveyId]);

  const handleOpenReview = (article: ReviewableArticle, type: "approve" | "deny") => {
    setActiveArticle(article);
    setDecision(type);
    
    // Attempt to parse existing jsonResponse to get a previously saved note
    let initialNote = "";
    if (article.jsonResponse) {
      try {
        const parsed = JSON.parse(article.jsonResponse);
        if (parsed.note) initialNote = parsed.note;
        if (parsed.decision) setDecision(parsed.decision);
      } catch (e) {
        initialNote = article.jsonResponse; // Fallback
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
      
      // Update local state to show it was reviewed
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

  if (loading) {
    return <div className="p-8 text-center text-gray-500">Loading articles...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Toaster position="top-right" />

      {/* Header */}
      <div className="border-b border-gray-200 bg-white">
        <div className="max-w-6xl mx-auto px-6 py-8">
          <button
            onClick={() => navigate(-1)}
            className="mb-4 inline-flex items-center gap-2 text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Surveys To Review
          </button>
          
          <div>
            <h1 className="text-3xl font-semibold text-gray-900">Review Articles</h1>
            <p className="text-sm text-gray-500 mt-1">
              Approve or deny articles assigned to you for this survey.
            </p>
          </div>
        </div>
      </div>

      {/* Articles List */}
      <div className="max-w-6xl mx-auto px-6 py-8">
        {articles.length === 0 ? (
          <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-12 text-center">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No Articles Found</h3>
            <p className="text-sm text-gray-500">There are no articles to review for this survey.</p>
          </div>
        ) : (
          <div className="space-y-6">
            {articles.map((article) => (
              <div key={article.externalId} className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h3 className="text-xl font-bold text-gray-900">{article.title}</h3>
                    <p className="text-sm text-gray-500 mt-1">
                      {article.journal} ({article.publicationYear}) - {article.authors.join(", ")}
                    </p>
                  </div>
                  {article.reviewed && (
                    <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Reviewed
                    </span>
                  )}
                </div>
                
                <div className="mb-6">
                  <h4 className="text-sm font-semibold text-gray-900 mb-2">Abstract</h4>
                  <p className="text-sm text-gray-700 whitespace-pre-line bg-gray-50 p-4 rounded-lg border border-gray-100">
                    {article.articleAbstract || "No abstract available."}
                  </p>
                </div>

                {article.aiAnnotations && article.aiAnnotations.length > 0 && (
                  <div className="mb-6">
                    <h4 className="text-sm font-semibold text-blue-900 mb-2 flex items-center gap-2">
                      <span className="w-4 h-4 bg-blue-100 text-blue-600 rounded flex items-center justify-center text-xs font-bold">AI</span>
                      AI-Generated Queries & Extraction
                    </h4>
                    <div className="space-y-4">
                      {article.aiAnnotations.map((annotation, idx) => {
                        let parsedData = null;
                        try { parsedData = JSON.parse(annotation.jsonResponse); } catch(e) { /* ignore */ }
                        return (
                          <div key={idx} className="bg-blue-50/50 p-4 rounded-lg border border-blue-100">
                            <p className="text-xs font-semibold text-blue-800 mb-2">Prompt: {annotation.promptText}</p>
                            {parsedData ? (
                              <pre className="text-xs text-gray-700 bg-white p-3 rounded border border-gray-200 overflow-auto whitespace-pre-wrap">
                                {JSON.stringify(parsedData, null, 2)}
                              </pre>
                            ) : (
                              <p className="text-xs text-gray-700 bg-white p-3 rounded border border-gray-200 overflow-auto whitespace-pre-wrap">
                                {annotation.jsonResponse}
                              </p>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                <div className="flex gap-3">
                  <button
                    onClick={() => handleOpenReview(article, "approve")}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-green-50 text-green-700 border border-green-200 hover:bg-green-100 rounded-lg font-medium transition-colors"
                  >
                    <CheckCircle className="w-4 h-4" />
                    Approve
                  </button>
                  <button
                    onClick={() => handleOpenReview(article, "deny")}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-red-50 text-red-700 border border-red-200 hover:bg-red-100 rounded-lg font-medium transition-colors"
                  >
                    <XCircle className="w-4 h-4" />
                    Deny
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Review Modal */}
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
                onClick={() => setActiveArticle(null)}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleSubmitReview}
                className={`px-4 py-2 text-sm font-medium text-white rounded-lg transition-colors ${
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
