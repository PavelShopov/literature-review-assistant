import { useState } from "react";
import { CheckCircle2, FileText, Eye, FolderPlus, XCircle } from "lucide-react";
import { motion } from "motion/react";
import { useNavigate } from "react-router";
import { toast } from "sonner";

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
  decisions: Record<string, {
    decision: "added" | "skipped";
    summary: string;
    decidedAt: string;
    reviewerName?: string;
  }>;
  onReviewReference: (referenceId: string, decision: "added" | "skipped", summary: string) => void;
  onAddToNewSurvey: (referenceId: string) => void;
}

export function ReferencesList({
  references,
  surveyId,
  decisions,
  onReviewReference,
  onAddToNewSurvey,
}: ReferencesListProps) {
  const navigate = useNavigate();
  const [summaries, setSummaries] = useState<Record<string, string>>({});

  const handleViewArticle = (referenceId: string) => {
    navigate(`/survey/${surveyId}/articles/${referenceId}/view`);
  };

  const handleAddToSurvey = (referenceId: string) => {
    const summary = summaries[referenceId]?.trim() ?? "";
    if (!summary) {
      toast.error("Write a short summary explaining why this article should be added");
      return;
    }

    onReviewReference(referenceId, "added", summary);
  };

  const handleSkip = (referenceId: string) => {
    onReviewReference(referenceId, "skipped", summaries[referenceId]?.trim() ?? "");
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
            {decisions[reference.id] && (
              <div
                className={`mb-4 inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-medium ${
                  decisions[reference.id].decision === "added"
                    ? "bg-green-50 text-green-700 border-green-200"
                    : "bg-gray-50 text-gray-700 border-gray-200"
                }`}
              >
                {decisions[reference.id].decision === "added" ? (
                  <CheckCircle2 className="w-3.5 h-3.5" />
                ) : (
                  <XCircle className="w-3.5 h-3.5" />
                )}
                {decisions[reference.id].decision === "added" ? "Added to survey" : "Not added"}
                {decisions[reference.id].reviewerName ? ` by ${decisions[reference.id].reviewerName}` : ""}
              </div>
            )}

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

            <div className="mb-4">
              <label
                htmlFor={`reference-summary-${reference.id}`}
                className="block text-sm font-medium text-gray-900 mb-2"
              >
                Inclusion summary
              </label>
              <textarea
                id={`reference-summary-${reference.id}`}
                value={summaries[reference.id] ?? decisions[reference.id]?.summary ?? ""}
                onChange={(event) =>
                  setSummaries({ ...summaries, [reference.id]: event.target.value })
                }
                rows={3}
                placeholder="Why should this article be added to the survey?"
                className="w-full px-4 py-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              />
            </div>

            {/* Source Label */}
            <div className="flex items-center gap-1.5 text-xs text-gray-500 mb-4">
              <FileText className="w-3.5 h-3.5" />
              <span>Source: Survey article</span>
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
                onClick={() => handleAddToSurvey(reference.id)}
                className="flex-1 inline-flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-green-700 bg-green-50 border border-green-200 rounded-lg hover:bg-green-100 transition-colors"
              >
                <CheckCircle2 className="w-4 h-4" />
                Add to Survey
              </button>
              <button
                onClick={() => handleSkip(reference.id)}
                className="flex-1 inline-flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-gray-700 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <XCircle className="w-4 h-4" />
                Do Not Add
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
