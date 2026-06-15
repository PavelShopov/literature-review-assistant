import { useState } from "react";
import { Mail, Plus, Trash2, UserRound, UsersRound } from "lucide-react";
import { motion } from "motion/react";

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
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    onAddContributor({ name: name.trim(), email: email.trim() });
    setName("");
    setEmail("");
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
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200">
            {contributors.length} reviewer{contributors.length !== 1 ? "s" : ""}
          </span>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {canManageReviewers && (
          <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-[1fr_1fr_auto] gap-3">
            <div>
              <label htmlFor="reviewer-name" className="block text-sm font-medium text-gray-900 mb-2">
                Name
              </label>
              <input
                id="reviewer-name"
                type="text"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="e.g., Ana Petrova"
                className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <div>
              <label htmlFor="reviewer-email" className="block text-sm font-medium text-gray-900 mb-2">
                Email
              </label>
              <input
                id="reviewer-email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="name@example.com"
                className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <div className="flex items-end">
              <button
                type="submit"
                className="w-full md:w-auto inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
              >
                <Plus className="w-4 h-4" />
                Add
              </button>
            </div>
          </form>
        )}

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
            <div className="rounded-lg border border-dashed border-gray-300 px-4 py-6 text-center">
              <p className="text-sm font-medium text-gray-900">No reviewers added yet</p>
              <p className="text-xs text-gray-500 mt-1">
                Add people who should help screen articles and use AI for this survey.
              </p>
            </div>
          )}
        </div>
      </div>
    </motion.div>
  );
}
