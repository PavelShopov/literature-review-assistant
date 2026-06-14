import { FileText, Eye, Plus, FolderPlus } from "lucide-react";
import { motion } from "motion/react";
import { useNavigate } from "react-router";

export interface Reference {
  id: string;
  title: string;
  authors: string[];
  journal: string;
  year: number;
  snippet: string;
  relevanceScore: number;
}

interface ReferencesListProps {
  references: Reference[];
  surveyId: string;
  onAddToSurvey: (referenceId: string, surveyId: string) => void;
  onAddToNewSurvey: (referenceId: string) => void;
}

export function ReferencesList({
  references,
  surveyId,
  onAddToSurvey,
  onAddToNewSurvey,
}: ReferencesListProps) {
  const navigate = useNavigate();

  const handleViewArticle = (referenceId: string) => {
    navigate(`/survey/${surveyId}/articles`);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.2 }}
      className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden"
    >
      {/* Header */}
      <div className="px-6 py-5 border-b border-gray-100">
        <div className="flex items-center gap-2">
          <FileText className="w-5 h-5 text-gray-600" />
          <h2 className="text-lg font-semibold text-gray-900">
            Top References
          </h2>
          <span className="text-sm text-gray-500">
            ({references.length} sources)
          </span>
        </div>
        <p className="text-xs text-gray-500 mt-1">
          Most relevant articles from your database
        </p>
      </div>

      {/* References */}
      <div className="divide-y divide-gray-100">
        {references.map((reference, index) => (
          <motion.div
            key={reference.id}
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.1 }}
            className="p-6 hover:bg-gray-50 transition-colors group"
          >
            {/* Reference Header */}
            <div className="flex items-start justify-between mb-3">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-2">
                  <span className="inline-flex items-center justify-center w-6 h-6 bg-blue-100 text-blue-700 rounded-full text-xs font-semibold">
                    {index + 1}
                  </span>
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700 border border-green-200">
                    {reference.relevanceScore}% relevant
                  </span>
                </div>
                <h3 className="text-base font-semibold text-gray-900 mb-2 group-hover:text-blue-600 transition-colors">
                  {reference.title}
                </h3>
                <p className="text-sm text-gray-600 mb-1">
                  {reference.authors.join(", ")}
                </p>
                <p className="text-sm text-gray-500 italic">
                  {reference.journal} • {reference.year}
                </p>
              </div>
            </div>

            {/* Snippet */}
            <div className="bg-blue-50 border-l-4 border-blue-400 rounded-r-lg p-4 mb-4">
              <p className="text-xs font-medium text-blue-900 mb-1.5 uppercase tracking-wide">
                Relevant Excerpt
              </p>
              <p className="text-sm text-gray-700 leading-relaxed italic">
                "{reference.snippet}"
              </p>
            </div>

            {/* Source Label */}
            <div className="flex items-center gap-1.5 text-xs text-gray-500 mb-4">
              <FileText className="w-3.5 h-3.5" />
              <span>Source: Uploaded PDF</span>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => handleViewArticle(reference.id)}
                className="flex-1 inline-flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <Eye className="w-4 h-4" />
                View Article
              </button>
              <button
                onClick={() => onAddToSurvey(reference.id, surveyId)}
                className="flex-1 inline-flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-blue-700 bg-blue-50 border border-blue-200 rounded-lg hover:bg-blue-100 transition-colors"
              >
                <Plus className="w-4 h-4" />
                Add to Survey
              </button>
              <button
                onClick={() => onAddToNewSurvey(reference.id)}
                className="inline-flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-purple-700 bg-purple-50 border border-purple-200 rounded-lg hover:bg-purple-100 transition-colors"
              >
                <FolderPlus className="w-4 h-4" />
                New Survey
              </button>
            </div>
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}
