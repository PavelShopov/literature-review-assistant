import { useState, useEffect, useRef } from "react";
import { X, Save, Plus, Trash2, Loader2 } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

interface ReviewerInput {
  email: string;
  name?: string; // Optional now, since we might only have an email
}

interface ReviewerSuggestion {
  id: string;
  name: string;
  email: string;
}

interface EditSurveyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (survey: {
    name: string;
    description: string;
    status: "In Progress" | "Completed" | "Draft";
    reviewers?: ReviewerInput[];
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
    reviewers: [] as ReviewerInput[],
  });

  const [emailInput, setEmailInput] = useState("");
  const [suggestions, setSuggestions] = useState<ReviewerSuggestion[]>([]);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (survey) {
      setFormData({
        name: survey.name,
        description: survey.description,
        status: survey.status,
        reviewers: [],
      });
    } else {
      setFormData({
        name: "",
        description: "",
        status: "Draft",
        reviewers: [],
      });
    }
    setEmailInput("");
    setSuggestions([]);
  }, [survey, isOpen]);

  // Search database when user types an email
  useEffect(() => {
    const delayDebounce = setTimeout(async () => {
      if (emailInput.trim().length > 2) {
        setIsLoadingSuggestions(true);
        try {
          const res = await fetch(`http://localhost:8080/api/surveys/reviewers/search?email=${encodeURIComponent(emailInput)}`);
          if (res.ok) {
            const data = await res.json();
            setSuggestions(data);
            setShowDropdown(data.length > 0);
          }
        } catch (err) {
          console.error("Error fetching reviewer suggestions:", err);
        } finally {
          setIsLoadingSuggestions(false);
        }
      } else {
        setSuggestions([]);
        setShowDropdown(false);
      }
    }, 300); // 300ms Debounce to protect backend from overwhelming requests

    return () => clearTimeout(delayDebounce);
  }, [emailInput]);

  // Close dropdown if clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      name: formData.name,
      description: formData.description,
      status: formData.status,
      reviewers: formData.reviewers.length > 0 ? formData.reviewers : undefined,
    });
    onClose();
  };

  const handleAddReviewer = (selectedReviewer?: ReviewerSuggestion) => {
    if (selectedReviewer) {
      // Added from suggestion dropdown click
      if (!formData.reviewers.some(r => r.email === selectedReviewer.email)) {
        setFormData({
          ...formData,
          reviewers: [...formData.reviewers, { email: selectedReviewer.email, name: selectedReviewer.name }],
        });
      }
    } else if (emailInput.trim() && emailInput.includes("@")) {
      // Manually typed raw email
      if (!formData.reviewers.some(r => r.email === emailInput.trim())) {
        setFormData({
          ...formData,
          reviewers: [...formData.reviewers, { email: emailInput.trim() }],
        });
      }
    }
    setEmailInput("");
    setShowDropdown(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleAddReviewer();
    }
  };

  const handleRemoveReviewer = (index: number) => {
    setFormData({
      ...formData,
      reviewers: formData.reviewers.filter((_, i) => i !== index),
    });
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

              {/* Reviewers Section - Only on Create */}
              {!survey && (
                  <div className="pt-4 border-t border-gray-200">
                    <label className="block text-sm font-medium text-gray-900 mb-2">
                      Assign Reviewers <span className="text-gray-500 text-xs font-normal">(Type email address)</span>
                    </label>

                    {/* Chosen Reviewers Badges */}
                    {formData.reviewers.length > 0 && (
                        <div className="flex flex-wrap gap-2 mb-3">
                          {formData.reviewers.map((reviewer, index) => (
                              <span key={index} className="inline-flex items-center gap-1.5 bg-blue-50 border border-blue-200 text-blue-700 px-2.5 py-1 rounded-md text-xs font-medium">
                        {reviewer.name ? `${reviewer.name} (${reviewer.email})` : reviewer.email}
                                <button
                                    type="button"
                                    onClick={() => handleRemoveReviewer(index)}
                                    className="hover:text-blue-900 rounded-full"
                                >
                          <X className="w-3 h-3" />
                        </button>
                      </span>
                          ))}
                        </div>
                    )}

                    {/* Autocomplete Input Container */}
                    <div ref={dropdownRef} className="relative">
                      <div className="relative flex items-center">
                        <input
                            type="text"
                            placeholder="Enter reviewer email..."
                            value={emailInput}
                            onChange={(e) => setEmailInput(e.target.value)}
                            onKeyDown={handleKeyDown}
                            className="w-full px-4 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 pr-10"
                        />
                        {isLoadingSuggestions && (
                            <Loader2 className="absolute right-3 w-4 h-4 text-gray-400 animate-spin" />
                        )}
                      </div>

                      {/* Dynamic Suggestions Dropdown Menu */}
                      <AnimatePresence>
                        {showDropdown && (
                            <motion.ul
                                initial={{ opacity: 0, y: -4 }}
                                animate={{ opacity: 1, y: 0 }}
                                exit={{ opacity: 0, y: -4 }}
                                className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto divide-y divide-gray-100"
                            >
                              {suggestions.map((user) => (
                                  <li key={user.id}>
                                    <button
                                        type="button"
                                        onClick={() => handleAddReviewer(user)}
                                        className="w-full text-left px-4 py-2.5 hover:bg-gray-50 flex flex-col transition-colors"
                                    >
                                      <span className="text-sm font-medium text-gray-900">{user.name}</span>
                                      <span className="text-xs text-gray-500">{user.email}</span>
                                    </button>
                                  </li>
                              ))}
                            </motion.ul>
                        )}
                      </AnimatePresence>
                    </div>

                    <button
                        type="button"
                        onClick={() => handleAddReviewer()}
                        disabled={!emailInput.trim() || !emailInput.includes("@")}
                        className="mt-2 inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 rounded-md hover:bg-blue-100 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <Plus className="w-3.5 h-3.5" /> Add Raw Email
                    </button>
                  </div>
              )}
            </div>

            {/* Footer */}
            <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50 rounded-b-xl">
              <button type="button" onClick={onClose} className="px-4 py-2 text-sm text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50">
                Cancel
              </button>
              <button type="submit" className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 shadow-sm">
                <Save className="w-4 h-4" />
                {survey ? "Save Changes" : "Create Survey"}
              </button>
            </div>
          </form>
        </motion.div>
      </div>
  );
}