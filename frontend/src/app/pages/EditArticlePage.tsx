import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router";
import { ArrowLeft, Save, X } from "lucide-react";
import { toast } from "sonner";
import { Toaster } from "sonner";
import type { ArticleStatus } from "../components/ArticleCard";
import { getArticle } from "../api/client";

export default function EditArticlePage() {
  const navigate = useNavigate();
  const { surveyId = "survey-001", articleId } = useParams();
  const [article, setArticle] = useState<any>(null);
  // const article = articleId ? mockArticles[articleId as keyof typeof mockArticles] : null;

  const [formData, setFormData] = useState({
    title: "",
    authors: "",
    journal: "",
    year: new Date().getFullYear(),
    doi: "",
    status: "PENDING" as ArticleStatus,
    abstract: "",
  });
  useEffect(() => {
    if (!articleId) return;

    getArticle(articleId)
      .then((data) => {
        setArticle(data);

        setFormData({
          title: data.title,
          authors: data.authors.join(", "),
          journal: data.journal,
          year: data.year,
          doi: data.doi,
          status: data.status,
          abstract: data.abstract || "",
        });
      })
      .catch((err) => console.error(err));
  }, [articleId]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    toast.success("Article updated successfully!");
    navigate(`/survey/${surveyId}`);
  };

  const handleCancel = () => {
    navigate(`/survey/${surveyId}`);
  };

  if (!article) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Toaster position="top-right" />
        <div className="text-center">
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Article not found</h2>
          <button
            onClick={handleCancel}
            className="text-sm text-blue-600 hover:text-blue-700"
          >
            Back to Survey
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Toaster position="top-right" />
      
      {/* Header */}
      <div className="border-b border-gray-200 bg-white sticky top-0 z-10">
        <div className="max-w-4xl mx-auto px-6 py-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <button
                onClick={handleCancel}
                className="inline-flex items-center justify-center w-8 h-8 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <div>
                <h1 className="text-xl font-semibold text-gray-900">Edit Article</h1>
                <p className="text-xs text-gray-500 mt-0.5">
                  ← Returns to Survey Dashboard
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={handleCancel}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <X className="w-4 h-4" />
                Cancel
              </button>
              <button
                onClick={handleSubmit}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
              >
                <Save className="w-4 h-4" />
                Save Changes
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Form */}
      <div className="max-w-4xl mx-auto px-6 py-8">
        <form onSubmit={handleSubmit} className="bg-white rounded-xl border border-gray-200 shadow-sm">
          <div className="p-6 space-y-6">
            {/* Title */}
            <div>
              <label htmlFor="title" className="block text-sm font-medium text-gray-900 mb-2">
                Title <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                id="title"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                required
                className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* Authors */}
            <div>
              <label htmlFor="authors" className="block text-sm font-medium text-gray-900 mb-2">
                Authors <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                id="authors"
                value={formData.authors}
                onChange={(e) => setFormData({ ...formData, authors: e.target.value })}
                placeholder="Smith, J., Doe, A., Johnson, B."
                required
                className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <p className="text-xs text-gray-500 mt-1">Separate multiple authors with commas</p>
            </div>

            {/* Journal and Year */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="journal" className="block text-sm font-medium text-gray-900 mb-2">
                  Journal <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  id="journal"
                  value={formData.journal}
                  onChange={(e) => setFormData({ ...formData, journal: e.target.value })}
                  required
                  className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <div>
                <label htmlFor="year" className="block text-sm font-medium text-gray-900 mb-2">
                  Year <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  id="year"
                  value={formData.year}
                  onChange={(e) => setFormData({ ...formData, year: parseInt(e.target.value) })}
                  min="1900"
                  max="2100"
                  required
                  className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* DOI */}
            <div>
              <label htmlFor="doi" className="block text-sm font-medium text-gray-900 mb-2">
                DOI
              </label>
              <input
                type="text"
                id="doi"
                value={formData.doi}
                onChange={(e) => setFormData({ ...formData, doi: e.target.value })}
                placeholder="10.1000/example.2024.001"
                className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* Status */}
            <div>
              <label className="block text-sm font-medium text-gray-900 mb-3">
                Review Status <span className="text-red-500">*</span>
              </label>
              <div className="flex gap-3">
                {(["INCLUDED", "EXCLUDED", "PENDING"] as ArticleStatus[]).map((status) => (
                  <button
                    key={status}
                    type="button"
                    onClick={() => setFormData({ ...formData, status })}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                      formData.status === status
                        ? status === "INCLUDED"
                          ? "bg-green-600 text-white"
                          : status === "EXCLUDED"
                          ? "bg-red-600 text-white"
                          : "bg-yellow-600 text-white"
                        : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                    }`}
                  >
                    {status}
                  </button>
                ))}
              </div>
            </div>

            {/* Abstract */}
            <div>
              <label htmlFor="abstract" className="block text-sm font-medium text-gray-900 mb-2">
                Abstract
              </label>
              <textarea
                id="abstract"
                value={formData.abstract}
                onChange={(e) => setFormData({ ...formData, abstract: e.target.value })}
                rows={6}
                className="w-full px-4 py-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              />
            </div>
          </div>

          {/* Form Footer */}
          <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50 rounded-b-xl">
            <button
              type="button"
              onClick={handleCancel}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
            >
              <Save className="w-4 h-4" />
              Save Changes
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
