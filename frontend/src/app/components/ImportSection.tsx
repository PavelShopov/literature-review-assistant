import { useState } from "react";
import { Link2, FileText, Upload, X } from "lucide-react";
import { motion } from "motion/react";

type ImportOption = "url" | "bibtex" | "pdf";

interface ImportSectionProps {
  onImport: (type: ImportOption, data: string | File) => void;
  isOpen: boolean;
  onClose: () => void;
}

export function ImportSection({ onImport, isOpen, onClose }: ImportSectionProps) {
  const [activeTab, setActiveTab] = useState<ImportOption>("url");
  const [urlInput, setUrlInput] = useState("");
  const [bibtexInput, setBibtexInput] = useState("");
  const [isDragging, setIsDragging] = useState(false);

  if (!isOpen) return null;

  const handleUrlImport = () => {
    if (urlInput.trim()) {
      onImport("url", urlInput);
      setUrlInput("");
    }
  };

  const handleBibtexImport = () => {
    if (bibtexInput.trim()) {
      onImport("bibtex", bibtexInput);
      setBibtexInput("");
    }
  };

  const handleFileUpload = (file: File) => {
    onImport("pdf", file);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file && file.type === "application/pdf") {
      handleFileUpload(file);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden"
    >
      <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
        <h2 className="text-lg font-semibold text-gray-900">Import Articles</h2>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-gray-600 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200">
        <button
          onClick={() => setActiveTab("url")}
          className={`flex-1 flex items-center justify-center gap-2 px-6 py-3 text-sm font-medium transition-colors ${
            activeTab === "url"
              ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50/50"
              : "text-gray-600 hover:text-gray-900 hover:bg-gray-50"
          }`}
        >
          <Link2 className="w-4 h-4" />
          URL / DOI
        </button>
        <button
          onClick={() => setActiveTab("bibtex")}
          className={`flex-1 flex items-center justify-center gap-2 px-6 py-3 text-sm font-medium transition-colors ${
            activeTab === "bibtex"
              ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50/50"
              : "text-gray-600 hover:text-gray-900 hover:bg-gray-50"
          }`}
        >
          <FileText className="w-4 h-4" />
          BibTeX
        </button>
        <button
          onClick={() => setActiveTab("pdf")}
          className={`flex-1 flex items-center justify-center gap-2 px-6 py-3 text-sm font-medium transition-colors ${
            activeTab === "pdf"
              ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50/50"
              : "text-gray-600 hover:text-gray-900 hover:bg-gray-50"
          }`}
        >
          <Upload className="w-4 h-4" />
          Upload PDF
        </button>
      </div>

      {/* Content */}
      <div className="p-6">
        {activeTab === "url" && (
          <div className="space-y-4">
            <div>
              <input
                type="text"
                value={urlInput}
                onChange={(e) => setUrlInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleUrlImport()}
                placeholder="https://doi.org/10.1000/example or article URL"
                className="w-full px-4 py-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <p className="text-xs text-gray-500 mt-2">
                Paste an article link or DOI to automatically import metadata
              </p>
            </div>
            <button
              onClick={handleUrlImport}
              disabled={!urlInput.trim()}
              className="w-full px-4 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
            >
              Import from URL
            </button>
          </div>
        )}

        {activeTab === "bibtex" && (
          <div className="space-y-4">
            <div>
              <textarea
                value={bibtexInput}
                onChange={(e) => setBibtexInput(e.target.value)}
                placeholder="@article{example2024,&#10;  title={Your Article Title},&#10;  author={Smith, John and Doe, Jane},&#10;  journal={Nature},&#10;  year={2024}&#10;}"
                rows={8}
                className="w-full px-4 py-3 text-sm font-mono border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              />
              <p className="text-xs text-gray-500 mt-2">
                Paste BibTeX entries to import multiple articles at once
              </p>
            </div>
            <button
              onClick={handleBibtexImport}
              disabled={!bibtexInput.trim()}
              className="w-full px-4 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
            >
              Import BibTeX
            </button>
          </div>
        )}

        {activeTab === "pdf" && (
          <div className="space-y-4">
            <div
              onDrop={handleDrop}
              onDragOver={(e) => {
                e.preventDefault();
                setIsDragging(true);
              }}
              onDragLeave={() => setIsDragging(false)}
              className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors ${
                isDragging
                  ? "border-blue-500 bg-blue-50"
                  : "border-gray-300 hover:border-gray-400"
              }`}
            >
              <Upload className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <p className="text-sm font-medium text-gray-900 mb-1">
                Drop PDF file here, or click to browse
              </p>
              <p className="text-xs text-gray-500">
                Upload a research paper PDF to extract metadata
              </p>
              <input
                type="file"
                accept="application/pdf"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) handleFileUpload(file);
                }}
                className="hidden"
                id="pdf-upload"
              />
              <label
                htmlFor="pdf-upload"
                className="inline-block mt-4 px-4 py-2 text-sm font-medium text-blue-600 hover:text-blue-700 cursor-pointer"
              >
                Browse files
              </label>
            </div>
          </div>
        )}
      </div>
    </motion.div>
  );
}
