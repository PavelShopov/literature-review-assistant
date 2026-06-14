import { MessageSquare } from "lucide-react";
import { motion } from "motion/react";

interface EmptyAskStateProps {
  articleCount: number;
}

export function EmptyAskState({ articleCount }: EmptyAskStateProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-gradient-to-br from-blue-50 to-purple-50 rounded-xl border border-gray-200 shadow-sm p-12 text-center"
    >
      <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
        <MessageSquare className="w-8 h-8 text-white" />
      </div>
      <h3 className="text-xl font-semibold text-gray-900 mb-2">
        Ask a question to explore your research
      </h3>
      <p className="text-sm text-gray-600 max-w-md mx-auto mb-6">
        Our AI will analyze your {articleCount} uploaded article{articleCount !== 1 ? "s" : ""} and provide 
        insights with relevant references and citations.
      </p>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 max-w-3xl mx-auto mt-8">
        <div className="bg-white rounded-lg p-4 text-left border border-gray-200">
          <div className="text-2xl mb-2">🔍</div>
          <p className="text-xs font-medium text-gray-900 mb-1">Deep Analysis</p>
          <p className="text-xs text-gray-600">Search across all your documents</p>
        </div>
        <div className="bg-white rounded-lg p-4 text-left border border-gray-200">
          <div className="text-2xl mb-2">📚</div>
          <p className="text-xs font-medium text-gray-900 mb-1">Referenced Answers</p>
          <p className="text-xs text-gray-600">Every answer includes sources</p>
        </div>
        <div className="bg-white rounded-lg p-4 text-left border border-gray-200">
          <div className="text-2xl mb-2">⚡</div>
          <p className="text-xs font-medium text-gray-900 mb-1">Instant Insights</p>
          <p className="text-xs text-gray-600">Get answers in seconds</p>
        </div>
      </div>
    </motion.div>
  );
}
