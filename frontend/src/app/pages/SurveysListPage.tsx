// import { useState } from "react";
import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Plus, Search, FileText, Calendar, Edit, Trash2 } from "lucide-react";
import { motion } from "motion/react";
import { toast } from "sonner";
import { Toaster } from "sonner";
import { EditSurveyModal } from "../components/EditSurveyModal";
import { deleteSurvey, getSurveys, saveSurvey } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export interface Survey {
  id: string;
  name: string;
  description: string;
  createdDate: string;
  status: "In Progress" | "Completed" | "Draft";
  totalArticles: number;
}

const statusConfig = {
  "In Progress": {
    bg: "bg-blue-100",
    text: "text-blue-700",
    border: "border-blue-200",
  },
  Completed: {
    bg: "bg-green-100",
    text: "text-green-700",
    border: "border-green-200",
  },
  Draft: {
    bg: "bg-gray-100",
    text: "text-gray-700",
    border: "border-gray-200",
  },
};

export default function SurveysListPage() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  // const [surveys, setSurveys] = useState<Survey[]>(mockSurveys);
  const [surveys, setSurveys] = useState<Survey[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingSurvey, setEditingSurvey] = useState<Survey | null>(null);
  useEffect(() => {
    getSurveys()
      .then(setSurveys)
      .catch((err) => console.error("Fetch error:", err));
  }, []);

  const filteredSurveys = surveys.filter((survey) =>
    survey.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    survey.description.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleAddSurvey = () => {
    setEditingSurvey(null);
    setIsEditModalOpen(true);
  };

  const handleEditSurvey = (survey: Survey, e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingSurvey(survey);
    setIsEditModalOpen(true);
  };

  const handleSaveSurvey = (surveyData: Omit<Survey, "id" | "createdDate" | "totalArticles">) => {
    if (editingSurvey) {
      // Update existing survey
      const updatedSurvey = { ...editingSurvey, ...surveyData };
      setSurveys(surveys.map((s) =>
        s.id === editingSurvey.id
          ? updatedSurvey
          : s
      ));
      saveSurvey(updatedSurvey).catch((err) => console.error("Save survey error:", err));
      toast.success("Survey updated successfully!");
    } else {
      // Create new survey
      const newSurvey: Survey = {
        id: `survey-${Date.now()}`,
        ...surveyData,
        createdDate: new Date().toISOString().split("T")[0],
        totalArticles: 0,
      };
      setSurveys([newSurvey, ...surveys]);
      saveSurvey(newSurvey).catch((err) => console.error("Save survey error:", err));
      toast.success("Survey created successfully!");
    }
    setIsEditModalOpen(false);
  };

  const handleDeleteSurvey = (surveyId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm("Are you sure you want to delete this survey? This action cannot be undone.")) {
      setSurveys(surveys.filter((s) => s.id !== surveyId));
      deleteSurvey(surveyId).catch((err) => console.error("Delete survey error:", err));
      toast.success("Survey deleted");
    }
  };

  const handleViewSurvey = (surveyId: string) => {
    navigate(`/survey/${surveyId}`);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Toaster position="top-right" />

      {/* Header */}
      <div className="border-b border-gray-200 bg-white">
        <div className="max-w-6xl mx-auto px-6 py-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-3xl font-semibold text-gray-900">Surveys</h1>
              <p className="text-sm text-gray-500 mt-1">
                Manage your systematic literature reviews
              </p>
            </div>
            <div className="flex items-center gap-3">
              {user && (
                <div className="hidden sm:block text-right">
                  <p className="text-sm font-medium text-gray-900">{user.name}</p>
                  <button
                    type="button"
                    onClick={() => logout().then(() => navigate("/login"))}
                    className="text-xs font-medium text-gray-500 hover:text-gray-900"
                  >
                    Sign out
                  </button>
                </div>
              )}
              <button
                onClick={handleAddSurvey}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
              >
                <Plus className="w-4 h-4" />
                New Survey
              </button>
            </div>
          </div>

          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search surveys..."
              className="w-full pl-10 pr-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>
      </div>

      {/* Surveys List */}
      <div className="max-w-6xl mx-auto px-6 py-8">
        {filteredSurveys.length === 0 ? (
          <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-12 text-center">
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <FileText className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              {searchQuery ? "No surveys found" : "No surveys yet"}
            </h3>
            <p className="text-sm text-gray-500 mb-6">
              {searchQuery
                ? "Try adjusting your search query"
                : "Create your first systematic review to get started"}
            </p>
            {!searchQuery && (
              <button
                onClick={handleAddSurvey}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
              >
                <Plus className="w-4 h-4" />
                Create Survey
              </button>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {filteredSurveys.map((survey) => {
              const status = statusConfig[survey.status];
              return (
                <motion.div
                  key={survey.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  onClick={() => handleViewSurvey(survey.id)}
                  className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer group"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-lg font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">
                          {survey.name}
                        </h3>
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${status.bg} ${status.text} ${status.border} border`}
                        >
                          {survey.status}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 mb-4 line-clamp-2">
                        {survey.description}
                      </p>
                      <div className="flex items-center gap-6 text-xs text-gray-500">
                        <div className="flex items-center gap-1.5">
                          <FileText className="w-3.5 h-3.5" />
                          {survey.totalArticles} article{survey.totalArticles !== 1 ? "s" : ""}
                        </div>
                        <div className="flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5" />
                          Created {new Date(survey.createdDate).toLocaleDateString()}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 ml-4">
                      <button
                        onClick={(e) => handleEditSurvey(survey, e)}
                        className="inline-flex items-center justify-center w-9 h-9 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        title="Edit survey"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={(e) => handleDeleteSurvey(survey.id, e)}
                        className="inline-flex items-center justify-center w-9 h-9 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Delete survey"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </motion.div>
              );
            })}
          </div>
        )}
      </div>

      {/* Edit Modal */}
      <EditSurveyModal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        onSave={handleSaveSurvey}
        survey={editingSurvey}
      />
    </div>
  );
}
