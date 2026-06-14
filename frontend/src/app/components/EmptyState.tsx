import { FileSearch } from "lucide-react";

interface EmptyStateProps {
  onAddArticle: () => void;
}

export function EmptyState({ onAddArticle }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-20 px-4">
      <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
        <FileSearch className="w-8 h-8 text-gray-400" />
      </div>
      <h3 className="text-lg font-semibold text-gray-900 mb-2">
        No articles yet
      </h3>
      <p className="text-sm text-gray-500 text-center max-w-md mb-6">
        Start building your systematic review by importing articles from URLs,
        BibTeX, or uploading PDF files.
      </p>
      <button
        onClick={onAddArticle}
        className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
      >
        Import your first article
      </button>
    </div>
  );
}
