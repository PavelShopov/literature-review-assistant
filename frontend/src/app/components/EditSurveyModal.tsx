import { useState, useEffect } from "react";
import { X, Save } from "lucide-react";
import { motion } from "motion/react";

interface EditSurveyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (survey: {
    name: string;
    description: string;
    status: "In Progress" | "Completed" | "Draft";
  }) => void;
  survey: {
    id: string;
    name: string;
    description: string;
    status: "In Progress" | "Completed" | "Draft";
  } | null;
}

export function EditSurveyModal({ isOpen, onClose, onSave, survey }: EditSurveyModalProps) {
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    status: "Draft" as "In Progress" | "Completed" | "Draft",
  });

  // Handle resetting or filling the form state when the modal opens/closes
  useEffect(() => {
    if (survey) {
      setFormData({
        name: survey.name,
        description: survey.description,
        status: survey.status,
      });
    } else {
      setFormData({
        name: "",
        description: "",
        status: "Draft",
      });
    }
  }, [survey, isOpen]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      name: formData.name,
      description: formData.description,
      status: formData.status,
    });
    onClose();
  };

  if (!isOpen) return null;

  return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
        <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-xl shadow-xl max-w-2xl w-full"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <h2 className="text-xl font-semibold text-gray-900">
              {survey ? "Edit Survey" : "Create New Survey"}
            </h2>
            <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600">
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit}>
            <div className="px-6 py-6 space-y-5">
              {/* Name */}
              <div>
                <label htmlFor="survey-name" className="block text-sm font-medium text-gray-900 mb-2">
                  Survey Name <span className="text-red-500">*</span>
                </label>
                <input
                    type="text"
                    id="survey-name"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    required
                    className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Description */}
              <div>
                <label htmlFor="survey-description" className="block text-sm font-medium text-gray-900 mb-2">
                  Description <span className="text-red-500">*</span>
                </label>
                <textarea
                    id="survey-description"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    rows={3}
                    required
                    className="w-full px-4 py-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                />
              </div>

              {/* Status */}
              <div>
                <label className="block text-sm font-medium text-gray-900 mb-3">Status *</label>
                <div className="flex gap-3">
                  {(["Draft", "In Progress", "Completed"] as const).map((status) => (
                      <button
                          key={status}
                          type="button"
                          onClick={() => setFormData({ ...formData, status })}
                          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                              formData.status === status
                                  ? "bg-blue-600 text-white"
                                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                          }`}
                      >
                        {status}
                      </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Footer */}
            <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50 rounded-b-xl">
              <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 text-sm text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                  type="submit"
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 shadow-sm"
              >
                <Save className="w-4 h-4" />
                {survey ? "Save Changes" : "Create Survey"}
              </button>
            </div>
          </form>
        </motion.div>
      </div>
  );
}