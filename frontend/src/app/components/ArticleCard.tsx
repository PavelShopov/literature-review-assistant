import { Eye, Edit, Trash2, ExternalLink, UserRound } from "lucide-react";
import { motion } from "motion/react";

export type ArticleStatus = "INCLUDED" | "EXCLUDED" | "PENDING";

export interface Article {
  id: string;
  title: string;
  authors: string[];
  journal: string;
  year: number;
  doi: string;
  status: ArticleStatus;
  abstract?: string;
  inclusionSummary?: string;
  addedBy?: {
    id: string;
    name: string;
    role: "Owner" | "Reviewer";
  };
}

interface ArticleCardProps {
  article: Article;
  onView: (id: string) => void;
  onEdit: (id: string) => void;
  onDelete: (id: string) => void;
  canEdit?: boolean;
  canDelete?: boolean;
}

const statusConfig = {
  INCLUDED: {
    bg: "bg-green-100",
    text: "text-green-700",
    border: "border-green-200",
  },
  EXCLUDED: {
    bg: "bg-red-100",
    text: "text-red-700",
    border: "border-red-200",
  },
  PENDING: {
    bg: "bg-yellow-100",
    text: "text-yellow-700",
    border: "border-yellow-200",
  },
};

export function ArticleCard({
  article,
  onView,
  onEdit,
  onDelete,
  canEdit = true,
  canDelete = true,
}: ArticleCardProps) {
  const status = statusConfig[article.status];

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      whileHover={{ y: -4 }}
      transition={{ duration: 0.2 }}
      className="bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-all overflow-hidden group"
    >
      <div className="p-6">
        {/* Status Badge */}
        <div className="flex items-center justify-between mb-4">
          <span
            className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${status.bg} ${status.text} ${status.border} border`}
          >
            {article.status}
          </span>
          <span className="text-xs text-gray-500">{article.year}</span>
        </div>

        {/* Title */}
        <h3
          onClick={() => onView(article.id)}
          className="text-base font-semibold text-gray-900 mb-3 line-clamp-2 cursor-pointer hover:text-blue-600 transition-colors"
        >
          {article.title}
        </h3>

        {/* Authors */}
        <p className="text-sm text-gray-600 mb-2 line-clamp-1">
          {article.authors.join(", ")}
        </p>

        {/* Journal */}
        <p className="text-sm text-gray-500 mb-3 italic">{article.journal}</p>

        {/* DOI */}
        {article.doi && (
          <a
            href={`https://doi.org/${article.doi}`}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700 mb-4"
          >
            <ExternalLink className="w-3 h-3" />
            {article.doi}
          </a>
        )}

        {article.inclusionSummary && (
          <div className="rounded-lg border border-green-200 bg-green-50 p-3 mb-4">
            <p className="text-xs font-medium text-green-800 mb-1">
              Inclusion summary
            </p>
            <p className="text-xs text-gray-700 line-clamp-3">
              {article.inclusionSummary}
            </p>
          </div>
        )}

        {article.addedBy && (
          <div className="flex items-center gap-1.5 text-xs text-gray-500 mb-4">
            <UserRound className="w-3.5 h-3.5" />
            <span>Added by {article.addedBy.name}</span>
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center gap-2 pt-4 border-t border-gray-100">
          <button
            onClick={() => onView(article.id)}
            className="flex-1 inline-flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-gray-700 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
          >
            <Eye className="w-4 h-4" />
            View
          </button>
          {canEdit && (
            <button
              onClick={() => onEdit(article.id)}
              className="flex-1 inline-flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-blue-700 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
            >
              <Edit className="w-4 h-4" />
              Edit
            </button>
          )}
          {canDelete && (
            <button
              onClick={() => onDelete(article.id)}
              className="inline-flex items-center justify-center px-3 py-2 text-sm font-medium text-red-600 bg-red-50 rounded-lg hover:bg-red-100 transition-colors"
              title="Remove article"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>
    </motion.div>
  );
}
