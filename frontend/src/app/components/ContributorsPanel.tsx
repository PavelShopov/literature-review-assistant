import React, { useState, useEffect } from "react";
import { Mail, Plus, Trash2, UserRound, UsersRound, X } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

export interface Contributor {
  id: string;
  name: string;
  email: string;
  role: "Owner" | "Reviewer";
  addedDate: string;
}

interface ContributorsPanelProps {
  contributors: Contributor[];
  onAddContributor: (contributor: Pick<Contributor, "name" | "email">) => void;
  onRemoveContributor: (contributorId: string) => void;
  canManageReviewers?: boolean;
}

export function ContributorsPanel({
  contributors,
  onAddContributor,
  onRemoveContributor,
  canManageReviewers = true,
}: ContributorsPanelProps) {
  const [showForm, setShowForm] = useState(false);
  const [emailInput, setEmailInput] = useState("");
  const [suggestions, setSuggestions] = useState<{id: string, name: string, email: string}[]>([]);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = React.useRef<HTMLDivElement>(null);

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
    }, 300);

    return () => clearTimeout(delayDebounce);
  }, [emailInput]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Close form on Escape key
  useEffect(() => {
    function handleEsc(e: KeyboardEvent) {
      if (e.key === "Escape") closeForm();
    }
    if (showForm) document.addEventListener("keydown", handleEsc);
    return () => document.removeEventListener("keydown", handleEsc);
  }, [showForm]);

  const closeForm = () => {
    setShowForm(false);
    setEmailInput("");
    setSuggestions([]);
    setShowDropdown(false);
  };

  const handleAddReviewer = (selectedReviewer?: {name: string, email: string}) => {
    if (selectedReviewer) {
      onAddContributor({ name: selectedReviewer.name, email: selectedReviewer.email });
    } else if (emailInput.trim() && emailInput.includes("@")) {
      onAddContributor({ name: emailInput.trim().split("@")[0] || "Unknown", email: emailInput.trim() });
    }
    closeForm();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleAddReviewer();
    }
  };

  const owner = contributors.find((contributor) => contributor.role === "Owner");
  const invitedReviewers = contributors.filter(
    (contributor) => contributor.role === "Reviewer",
  );

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden"
    >
      {/* Header */}
      <div className="px-6 py-5 border-b border-gray-100">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <UsersRound className="w-5 h-5 text-gray-600" />
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Reviewers</h2>
              <p className="text-xs text-gray-500 mt-1">
                People listed here can view this survey, review articles, and use AI.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200">
              {contributors.length} reviewer{contributors.length !== 1 ? "s" : ""}
            </span>
            {canManageReviewers && !showForm && (
              <button
                type="button"
                onClick={() => setShowForm(true)}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
              >
                <Plus className="w-4 h-4" />
                Add Reviewer
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="p-6 space-y-5">
        {/* Add Reviewer Form — shown only when toggled */}
        <AnimatePresence>
          {canManageReviewers && showForm && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.2 }}
              className="overflow-hidden"
            >
              <div className="bg-blue-50 rounded-xl p-5 border border-blue-200">
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <p className="text-sm font-semibold text-gray-900">Assign a Reviewer</p>
                    <p className="text-xs text-gray-500 mt-0.5">Search by email — existing accounts will appear as suggestions.</p>
                  </div>
                  <button
                    type="button"
                    onClick={closeForm}
                    className="p-1.5 text-gray-400 hover:text-gray-700 hover:bg-white rounded-lg transition-colors"
                    title="Close"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                <div ref={dropdownRef} className="relative">
                  <div className="relative flex items-center gap-2">
                    <input
                      autoFocus
                      type="text"
                      placeholder="Enter reviewer email..."
                      value={emailInput}
                      onChange={(e) => setEmailInput(e.target.value)}
                      onKeyDown={handleKeyDown}
                      className="flex-1 px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                    />
                    {isLoadingSuggestions && (
                      <div className="absolute right-3">
                        <div className="w-4 h-4 border-2 border-gray-300 border-t-blue-600 rounded-full animate-spin" />
                      </div>
                    )}
                  </div>

                  {showDropdown && (
                    <ul className="absolute z-20 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto divide-y divide-gray-100">
                      {suggestions.map((user) => (
                        <li key={user.id}>
                          <button
                            type="button"
                            onClick={() => handleAddReviewer(user)}
                            className="w-full text-left px-4 py-2.5 hover:bg-blue-50 flex flex-col transition-colors"
                          >
                            <span className="text-sm font-medium text-gray-900">{user.name}</span>
                            <span className="text-xs text-gray-500">{user.email}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>

                <div className="flex items-center gap-2 mt-3">
                  <button
                    type="button"
                    onClick={() => handleAddReviewer()}
                    disabled={!emailInput.trim() || !emailInput.includes("@")}
                    className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                  >
                    <Plus className="w-4 h-4" /> Add Reviewer
                  </button>
                  <button
                    type="button"
                    onClick={closeForm}
                    className="px-4 py-2 text-sm font-medium text-gray-600 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Reviewer List */}
        <div className="space-y-3">
          {owner && (
            <div className="flex items-center justify-between gap-4 rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
              <div className="flex items-center gap-3 min-w-0">
                <div className="w-9 h-9 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center">
                  <UserRound className="w-4 h-4" />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">{owner.name}</p>
                  <p className="text-xs text-gray-500 truncate">{owner.email}</p>
                </div>
              </div>
              <span className="shrink-0 inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-700 border border-blue-200">
                Owner
              </span>
            </div>
          )}

          {invitedReviewers.map((contributor) => (
            <div
              key={contributor.id}
              className="flex items-center justify-between gap-4 rounded-lg border border-gray-200 px-4 py-3"
            >
              <div className="flex items-center gap-3 min-w-0">
                <div className="w-9 h-9 rounded-full bg-gray-100 text-gray-600 flex items-center justify-center">
                  <Mail className="w-4 h-4" />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">{contributor.name}</p>
                  <p className="text-xs text-gray-500 truncate">{contributor.email}</p>
                </div>
              </div>
              {canManageReviewers && (
                <button
                  type="button"
                  onClick={() => onRemoveContributor(contributor.id)}
                  className="shrink-0 inline-flex items-center justify-center w-9 h-9 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                  title="Remove reviewer"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </div>
          ))}

          {invitedReviewers.length === 0 && (
            <div className="rounded-lg border border-dashed border-gray-300 px-4 py-8 text-center">
              <UsersRound className="w-8 h-8 text-gray-300 mx-auto mb-2" />
              <p className="text-sm font-medium text-gray-900">No reviewers added yet</p>
              <p className="text-xs text-gray-500 mt-1">
                {canManageReviewers
                  ? "Click \"Add Reviewer\" above to assign people to this survey."
                  : "The survey owner hasn't added any reviewers yet."}
              </p>
            </div>
          )}
        </div>
      </div>
    </motion.div>
  );
}
