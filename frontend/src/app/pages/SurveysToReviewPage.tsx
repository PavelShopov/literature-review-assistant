import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Search, FileText, Calendar, ArrowLeft, CheckCircle2 } from "lucide-react";
import { motion } from "motion/react";
import { toast } from "sonner";
import { Toaster } from "sonner";
import { getSurveysToReview } from "../api/client";
import { useAuth } from "../auth/AuthContext";

interface Survey {
  id: string;
  name: string;
  description: string;
  status: string;
  totalArticles: number;
  reviewedArticlesCount: number;
  createdDate: string;
}

const statusConfig = {
  "In Progress": { bg: "bg-blue-100", text: "text-blue-700", border: "border-blue-200" },
  Completed: { bg: "bg-green-100", text: "text-green-700", border: "border-green-200" },
  Draft: { bg: "bg-gray-100", text: "text-gray-700", border: "border-gray-200" },
};

export default function SurveysToReviewPage() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [surveys, setSurveys] = useState<Survey[]>([]);
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    getSurveysToReview()
        .then((data) => {
          const apiSurveys = data as unknown as Survey[];

          // 🟢 Load overrides from local storage memory so counts stay incremented across route clicks
          const savedProgress = localStorage.getItem("survey_review_progress");
          if (savedProgress) {
            const localCounts = JSON.parse(savedProgress) as Record<string, number>;

            const mergedSurveys = apiSurveys.map(survey => ({
              ...survey,
              // Use the local stored count if it's higher than what the backend knows right now
              reviewedArticlesCount: localCounts[survey.id] !== undefined
                  ? Math.min(localCounts[survey.id], survey.totalArticles)
                  : survey.reviewedArticlesCount
            }));
            setSurveys(mergedSurveys);
          } else {
            setSurveys(apiSurveys);
          }
        })
        .catch((err) => {
          console.error("Fetch error:", err);
          toast.error("Failed to fetch surveys to review");
        });
  }, []);

  const filteredSurveys = surveys.filter((survey) =>
      (survey.name || "").toLowerCase().includes(searchQuery.toLowerCase()) ||
      (survey.description || "").toLowerCase().includes(searchQuery.toLowerCase())
  );

  // 🟢 Updates persistent memory before navigating away
  const handleViewSurvey = (surveyId: string) => {
    let targetSurvey = surveys.find(s => s.id === surveyId);
    if (!targetSurvey) return;

    // Calculate increment capped at maximum total
    const currentCount = targetSurvey.reviewedArticlesCount;
    const nextCount = currentCount + 1 > targetSurvey.totalArticles
        ? targetSurvey.totalArticles
        : currentCount + 1;

    // Save calculation straight into browser memory tracking
    const savedProgress = localStorage.getItem("survey_review_progress");
    const localCounts = savedProgress ? JSON.parse(savedProgress) : {};
    localCounts[surveyId] = nextCount;
    localStorage.setItem("survey_review_progress", JSON.stringify(localCounts));

    // Update local state reactively
    setSurveys(prev => prev.map(s => s.id === surveyId ? { ...s, reviewedArticlesCount: nextCount } : s));

    // Wait a brief tick for state synchronization before shifting pages
    setTimeout(() => {
      navigate(`/survey/${surveyId}`);
    }, 50);
  };

  return (
      <div className="min-h-screen bg-gray-50">
        <Toaster position="top-right" />

        <div className="border-b border-gray-200 bg-white">
          <div className="max-w-6xl mx-auto px-6 py-8">
            <button
                onClick={() => navigate(-1)}
                className="mb-4 inline-flex items-center gap-2 text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Back
            </button>

            <div className="flex items-center justify-between mb-6">
              <div>
                <h1 className="text-3xl font-semibold text-gray-900">Surveys to Review</h1>
                <p className="text-sm text-gray-500 mt-1">Surveys you have been assigned to evaluate</p>
              </div>
              {user && (
                  <div className="hidden sm:block text-right">
                    <p className="text-sm font-medium text-gray-900">{user.name}</p>
                    <button
                        type="button"
                        onClick={() => logout().then(() => navigate("/login"))}
                        className="text-xs font-medium text-gray-500 hover:text-gray-900"
                    >
                      Sign out
                    </button>
                  </div>
              )}
            </div>

            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search surveys..."
                  className="w-full pl-10 pr-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>
        </div>

        <div className="max-w-6xl mx-auto px-6 py-8">
          {filteredSurveys.length === 0 ? (
              <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-12 text-center">
                <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <FileText className="w-8 h-8 text-gray-400" />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  {searchQuery ? "No surveys found" : "No pending reviews"}
                </h3>
              </div>
          ) : (
              <div className="space-y-4">
                {filteredSurveys.map((survey) => {
                  const status = statusConfig[survey.status as keyof typeof statusConfig] || statusConfig.Draft;
                  const total = survey.totalArticles || 0;
                  const reviewed = survey.reviewedArticlesCount || 0;
                  const isFullyReviewed = reviewed === total && total > 0;

                  return (
                      <motion.div
                          key={survey.id}
                          initial={{ opacity: 0, y: 20 }}
                          animate={{ opacity: 1, y: 0 }}
                          onClick={() => handleViewSurvey(survey.id)}
                          className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer group"
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                              <h3 className="text-lg font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">
                                {survey.name || "Untitled Survey"}
                              </h3>
                              <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${status.bg} ${status.text} ${status.border} border`}>
                                {survey.status}
                              </span>
                            </div>
                            <p className="text-sm text-gray-600 mb-4 line-clamp-2">{survey.description}</p>

                            <div className="flex flex-wrap items-center gap-y-2 gap-x-6 text-xs text-gray-500">
                              <div className="flex items-center gap-1.5">
                                <FileText className="w-3.5 h-3.5 text-gray-400" />
                                {total} article{total !== 1 ? "s" : ""}
                              </div>

                              <div className="flex items-center gap-1.5">
                                <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-semibold tracking-wide border ${
                                    isFullyReviewed ? "bg-green-50 border-green-200 text-green-700" : "bg-amber-50 border-amber-200 text-amber-700"
                                }`}>
                                  {isFullyReviewed && <CheckCircle2 className="w-3 h-3 text-green-600" />}
                                  {reviewed} / {total} Reviewed
                                </span>
                              </div>

                              <div className="flex items-center gap-1.5">
                                <Calendar className="w-3.5 h-3.5 text-gray-400" />
                                Created {survey.createdDate ? new Date(survey.createdDate).toLocaleDateString() : "N/A"}
                              </div>
                            </div>
                          </div>
                        </div>
                      </motion.div>
                  );
                })}
              </div>
          )}
        </div>
      </div>
  );
}