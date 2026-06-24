import { useEffect, useState } from "react";
import { ExternalLink, FileText } from "lucide-react";
import { useNavigate, useParams } from "react-router";
import { toast } from "sonner";
import { getArticle, openArticleResource, updateSurveyArticle } from "../api/client";
import type { Article, ArticleStatus } from "../components/ArticleCard";

export default function ArticleDetails() {
  const { surveyId, articleId } = useParams<{ surveyId: string; articleId: string }>();
  const navigate = useNavigate();
  const [article, setArticle] = useState<Article | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!articleId) return;
    setLoading(true);
    getArticle(articleId)
      .then(setArticle)
      .catch((err) => setError(err instanceof Error ? err.message : "Article could not be loaded"))
      .finally(() => setLoading(false));
  }, [articleId]);

  const open = (preferPdf = false) => {
    if (!article) return;
    openArticleResource(article, preferPdf)
      .catch((err) => toast.error(err instanceof Error ? err.message : "Article could not be opened"));
  };

  const updateStatus = (status: ArticleStatus) => {
    if (!article || !surveyId) return;

    updateSurveyArticle(surveyId, article.id, {
      title: article.title,
      authors: article.authors,
      journal: article.journal,
      year: article.year,
      doi: article.doi,
      status,
      abstract: article.abstract ?? "",
      inclusionSummary: article.inclusionSummary ?? "",
    })
      .then((updated) => {
        setArticle(updated);
        toast.success(`Status updated to ${status}`);
      })
      .catch((err) => toast.error(err instanceof Error ? err.message : "Status update failed"));
  };

  if (loading) {
    return <div className="flex min-h-screen items-center justify-center text-gray-600">Loading article...</div>;
  }

  if (error || !article) {
    return (
      <div className="mx-auto mt-10 max-w-4xl rounded-lg border border-red-200 bg-red-50 p-6">
        <h2 className="mb-2 text-xl font-bold text-red-700">Article failed to load</h2>
        <p className="text-red-600">{error ?? "Article not found"}</p>
        <button
          onClick={() => navigate(`/survey/${surveyId}`)}
          className="mt-4 rounded bg-red-600 px-4 py-2 text-white hover:bg-red-700"
        >
          Back to survey
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto my-10 max-w-5xl rounded-xl border border-gray-100 bg-white p-8 shadow-md">
      <button
        onClick={() => navigate(`/survey/${surveyId}`)}
        className="mb-6 text-sm font-medium text-indigo-600 hover:text-indigo-800"
      >
        ← Back to survey
      </button>

      <div className="border-b border-gray-200 pb-6">
        <span className="inline-flex rounded-full bg-yellow-100 px-3 py-1 text-sm font-semibold text-yellow-800">
          {article.status}
        </span>
        <div className="mt-4 flex flex-wrap gap-2">
          {(["INCLUDED", "EXCLUDED", "PENDING"] as ArticleStatus[]).map((status) => (
            <button
              key={status}
              type="button"
              onClick={() => updateStatus(status)}
              className={`rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
                article.status === status
                  ? status === "INCLUDED"
                    ? "border-green-600 bg-green-600 text-white"
                    : status === "EXCLUDED"
                      ? "border-red-600 bg-red-600 text-white"
                      : "border-yellow-600 bg-yellow-600 text-white"
                  : "border-gray-200 bg-gray-50 text-gray-700 hover:bg-gray-100"
              }`}
            >
              {status}
            </button>
          ))}
        </div>
        <h1
          onClick={() => article.openUrl && open()}
          className={`mt-3 text-3xl font-extrabold text-gray-900 ${article.openUrl ? "cursor-pointer hover:text-blue-700" : ""}`}
        >
          {article.title}
        </h1>
        <p className="mt-2 text-gray-500">
          {article.journal || "Unknown publication"} {article.year ? `(${article.year})` : ""}
        </p>

        <div className="mt-5 flex flex-wrap gap-3">
          {article.openUrl && (
            <button
              onClick={() => open()}
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              <ExternalLink className="h-4 w-4" />
              Open article
            </button>
          )}
          {article.hasPdf && (
            <button
              onClick={() => open(true)}
              className="inline-flex items-center gap-2 rounded-lg bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100"
            >
              <FileText className="h-4 w-4" />
              Open PDF
            </button>
          )}
          {article.url && (
            <a
              href={article.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
            >
              <ExternalLink className="h-4 w-4" />
              Publisher page
            </a>
          )}
        </div>
      </div>

      <div className="mt-6">
        <h3 className="text-lg font-bold text-gray-900">Authors</h3>
        <p className="mt-2 text-sm text-gray-700">
          {article.authors?.length ? article.authors.join(", ") : "No authors available"}
        </p>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 rounded-lg border border-gray-100 bg-gray-50 p-4 md:grid-cols-2">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400">DOI</span>
          <p className="text-sm font-medium text-gray-800">{article.doi || "N/A"}</p>
        </div>
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400">Resource</span>
          <p className="text-sm font-medium text-gray-800">{article.openType || "N/A"}</p>
        </div>
      </div>

      <div className="mt-8">
        <h3 className="border-b pb-2 text-xl font-bold text-gray-900">Abstract</h3>
        <p className="mt-3 whitespace-pre-line text-justify leading-relaxed text-gray-700">
          {article.abstract || "No abstract is available for this article."}
        </p>
      </div>
    </div>
  );
}
