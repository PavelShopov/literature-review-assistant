import { Loader2 } from "lucide-react";
import { motion } from "motion/react";

export function LoadingState() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-xl border border-gray-200 shadow-sm p-8"
    >
      <div className="flex flex-col items-center justify-center py-8">
        <div className="relative mb-6">
          <div className="w-16 h-16 bg-gradient-to-br from-blue-100 to-purple-100 rounded-full flex items-center justify-center">
            <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
          </div>
          <div className="absolute inset-0 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full blur-xl opacity-20 animate-pulse" />
        </div>
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          Analyzing documents...
        </h3>
        <p className="text-sm text-gray-500 text-center max-w-md">
          AI is reading through your uploaded articles to find the most relevant information
        </p>
        
        {/* Progress indicators */}
        <div className="mt-8 space-y-2 w-full max-w-xs">
          {["Scanning articles", "Finding relevant passages", "Generating answer"].map((step, index) => (
            <motion.div
              key={step}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.3 }}
              className="flex items-center gap-3 text-xs text-gray-600"
            >
              <div className="w-1.5 h-1.5 bg-blue-600 rounded-full animate-pulse" />
              {step}
            </motion.div>
          ))}
        </div>
      </div>
    </motion.div>
  );
}
