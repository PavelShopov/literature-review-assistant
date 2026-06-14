import { X, ExternalLink } from "lucide-react";
import { motion } from "motion/react";
import type { Article, ArticleStatus } from "./ArticleCard";

interface ArticleModalProps {
  article: Article | null;
  isOpen: boolean;
  onClose: () => void;
  onUpdateStatus: (id: string, status: ArticleStatus) => void;
}

export function ArticleModal({
  article,
  isOpen,
  onClose,
  onUpdateStatus,
}: ArticleModalProps) {
  if (!isOpen || !article) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="bg-white rounded-xl shadow-xl max-w-3xl w-full max-h-[90vh] overflow-hidden flex flex-col"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">
            Article Details
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
          {/* Title */}
          <div>
            <h3 className="text-2xl font-semibold text-gray-900 mb-2">
              {article.title}
            </h3>
          </div>

          {/* Metadata */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                Authors
              </label>
              <p className="text-sm text-gray-900 mt-1">
                {article.authors.join(", ")}
              </p>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                Year
              </label>
              <p className="text-sm text-gray-900 mt-1">{article.year}</p>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                Journal
              </label>
              <p className="text-sm text-gray-900 mt-1">{article.journal}</p>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                DOI
              </label>
              <a
                href={`https://doi.org/${article.doi}`}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 text-sm text-blue-600 hover:text-blue-700 mt-1"
              >
                {article.doi}
                <ExternalLink className="w-3 h-3" />
              </a>
            </div>
          </div>

          {/* Abstract */}
          {article.abstract && (
            <div>
              <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                Abstract
              </label>
              <p className="text-sm text-gray-700 mt-2 leading-relaxed">
                {article.abstract}
              </p>
            </div>
          )}

          {/* Status */}
          <div>
            <label className="text-xs font-medium text-gray-500 uppercase tracking-wide block mb-3">
              Review Status
            </label>
            <div className="flex gap-3">
              {(["INCLUDED", "EXCLUDED", "PENDING"] as ArticleStatus[]).map(
                (status) => (
                  <button
                    key={status}
                    onClick={() => onUpdateStatus(article.id, status)}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                      article.status === status
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
                )
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Close
          </button>
        </div>
      </motion.div>
    </div>
  );
}
