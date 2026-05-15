import { useState } from "react";
import { Sparkles, Loader2 } from "lucide-react";
import { motion } from "motion/react";

interface AskInputProps {
  onAsk: (question: string) => void;
  isLoading: boolean;
}

export function AskInput({ onAsk, isLoading }: AskInputProps) {
  const [question, setQuestion] = useState("");

  const handleSubmit = () => {
    if (question.trim() && !isLoading) {
      onAsk(question);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) {
      handleSubmit();
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden"
    >
      <div className="px-6 py-5 border-b border-gray-100">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center">
            <Sparkles className="w-4 h-4 text-white" />
          </div>
          <h2 className="text-lg font-semibold text-gray-900">Ask a Question</h2>
        </div>
      </div>
      <div className="p-6 space-y-4">
        <textarea
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Ask a question about your uploaded articles…&#10;&#10;Examples:&#10;• What are the main findings on AI effectiveness in healthcare?&#10;• Which studies discuss ethical concerns?&#10;• Summarize the methodologies used across papers"
          rows={6}
          disabled={isLoading}
          className="w-full px-4 py-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none disabled:bg-gray-50 disabled:text-gray-500"
        />
        <div className="flex items-center justify-between">
          <p className="text-xs text-gray-500">
            💡 Answers are generated from your uploaded PDFs only
          </p>
          <button
            onClick={handleSubmit}
            disabled={!question.trim() || isLoading}
            className="inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium text-white bg-gradient-to-r from-blue-600 to-purple-600 rounded-lg hover:from-blue-700 hover:to-purple-700 disabled:from-gray-300 disabled:to-gray-300 disabled:cursor-not-allowed transition-all shadow-sm"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Analyzing...
              </>
            ) : (
              <>
                <Sparkles className="w-4 h-4" />
                Ask AI
              </>
            )}
          </button>
        </div>
        <p className="text-xs text-gray-400">
          Press <kbd className="px-1.5 py-0.5 bg-gray-100 rounded text-xs">⌘</kbd> + <kbd className="px-1.5 py-0.5 bg-gray-100 rounded text-xs">Enter</kbd> to submit
        </p>
      </div>
    </motion.div>
  );
}
