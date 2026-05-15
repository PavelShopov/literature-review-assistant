import { ArrowLeft, Plus } from "lucide-react";

interface HeaderProps {
  surveyName: string;
  onBack: () => void;
}

export function Header({ surveyName, onBack }: HeaderProps) {
  return (
    <div className="border-b border-gray-200 bg-white sticky top-0 z-10">
      <div className="max-w-7xl mx-auto px-6 py-5">
        <div className="flex items-center gap-3 mb-2">
          <button
            onClick={onBack}
            className="inline-flex items-center justify-center w-9 h-9 rounded-lg text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-semibold text-gray-900">Edit Article</h1>
            <p className="text-xs text-gray-500 mt-0.5">
              {surveyName}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}