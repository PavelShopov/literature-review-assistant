import { CheckCircle2, Sparkles } from "lucide-react";
import { motion } from "motion/react";

interface AIAnswerProps {
  question: string;
  answer: string;
  sourceCount: number;
  confidence?: number;
}

export function AIAnswer({ question, answer, sourceCount, confidence = 85 }: AIAnswerProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden"
    >
      {/* Header */}
      <div className="px-6 py-5 bg-gradient-to-r from-blue-50 to-purple-50 border-b border-gray-100">
        <div className="flex items-start justify-between">
          <div className="flex items-start gap-3">
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center mt-0.5">
              <Sparkles className="w-4 h-4 text-white" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900 mb-1">AI Answer</h2>
              <p className="text-sm text-gray-600 italic">"{question}"</p>
            </div>
          </div>
          <div className="flex items-center gap-2 text-xs">
            <div className="flex items-center gap-1.5 px-2.5 py-1 bg-white rounded-full border border-green-200">
              <CheckCircle2 className="w-3.5 h-3.5 text-green-600" />
              <span className="text-green-700 font-medium">{confidence}% confidence</span>
            </div>
          </div>
        </div>
      </div>

      {/* Answer Content */}
      <div className="px-6 py-6">
        <div className="prose prose-sm max-w-none">
          <p className="text-gray-700 leading-relaxed whitespace-pre-line">
            {answer}
          </p>
        </div>
        
        {/* Metadata */}
        <div className="mt-6 pt-4 border-t border-gray-100">
          <p className="text-xs text-gray-500">
            {sourceCount > 0 ? (
              <>
                Based on <span className="font-medium text-gray-700">{sourceCount} sources</span> from your survey database
              </>
            ) : (
              <>
                No survey database sources found. This answer is based on model knowledge only.
              </>
            )}
          </p>
        </div>
      </div>
    </motion.div>
  );
}
