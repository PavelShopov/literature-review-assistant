import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import { Sparkles, CheckCircle, ExternalLink, Calendar, BookOpen, User, Copy, HelpCircle, RefreshCw } from 'lucide-react';
import { toast, Toaster } from 'sonner';
import { getArticle } from '../api/client'; // Adjust this relative path to match your folder structure

interface LlmClassification {
    values: string[] | null;
    confidence: 'high' | 'medium' | 'low' | string | null;
    proof: string | null;
}

interface ArticleDto {
    externalId?: string;
    id?: string;
    title: string;
    journal: string;
    publicationYear?: number;
    year?: number;
    doi: string;
    url: string;
    status: string;
    articleAbstract?: string;
    abstractText?: string;
    inclusionSummary?: string;
    addedById?: string;
    addedByName?: string;
    addedByRole?: string;
    addedBy?: {
        id?: string | number;
        name?: string;
        role?: string;
    };
    authors: string[];
    llm_classifications?: Record<string, LlmClassification>;
}

export default function ArticleDetails() {
    const { surveyId, articleId } = useParams<{ surveyId: string; articleId: string }>();
    const navigate = useNavigate();

    const [article, setArticle] = useState<ArticleDto | null>(null);
    const [aiSuggestions, setAiSuggestions] = useState<Record<string, LlmClassification>>({});
    const [loading, setLoading] = useState<boolean>(true);
    const [generating, setGenerating] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        async function fetchArticleDetails() {
            try {
                setLoading(true);
                if (!articleId) throw new Error("Article ID is missing from the URL params.");

                const data = (await getArticle(articleId)) as unknown as ArticleDto;
                setArticle(data);
                if (data.llm_classifications) {
                    setAiSuggestions(data.llm_classifications);
                }
            } catch (err: any) {
                setError(err.message || "Something went wrong.");
            } finally {
                setLoading(false);
            }
        }

        if (articleId) {
            fetchArticleDetails();
        }
    }, [articleId]);

    const generateTaxonomyWithGemma = async () => {
        if (!article || !articleId) return;
        try {
            setGenerating(true);
            toast.loading("Gemma is reading the paper details...", { id: "gemma-task" });

            const targetPrompt = `
You are a research paper analysis assistant. Analyze the following paper details:
Title: "${article.title}"
Abstract: "${article.articleAbstract ?? article.abstractText ?? ''}"

Extract contextual dimensions mapping values, confidence level ('high', 'medium', 'low'), and a verbatim proof quote supporting your classification. You MUST respond with a valid JSON object ONLY. Do not include markdown code block formatting (such as \`\`\`json).

The output format must strictly follow this JSON schema structure:
{
  "Research Methodology": {
    "values": ["string"],
    "confidence": "high" | "medium" | "low",
    "proof": "Verbatim quote supporting your classification"
  },
  "Target Dataset Context": {
    "values": ["string"],
    "confidence": "high" | "medium" | "low",
    "proof": "Verbatim quote supporting your classification"
  }
}
`.trim();

            const response = await fetch(`http://localhost:8080/api/articles/${articleId}/analyze-gemma`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ prompt: targetPrompt })
            });

            if (!response.ok) {
                throw new Error(`Backend service returned error status: ${response.status}`);
            }

            const payload = await response.json();
            setAiSuggestions(payload);
            toast.success("Taxonomy suggestions populated successfully!", { id: "gemma-task" });
        } catch (err: any) {
            console.error(err);
            toast.error(`Gemma Extraction Failed: ${err.message || err}`, { id: "gemma-task" });
        } finally {
            setGenerating(false);
        }
    };

    const handleCopyText = (text: string, dimensionName: string) => {
        navigator.clipboard.writeText(text);
        toast.success(`Copied AI proof for "${dimensionName}" to clipboard!`);
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen bg-gray-50">
                <div className="flex flex-col items-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
                    <span className="ml-3 mt-4 text-lg text-gray-600 font-medium">Loading article details...</span>
                </div>
            </div>
        );
    }

    if (error || !article) {
        return (
            <div className="max-w-4xl mx-auto mt-10 p-6 bg-red-50 rounded-xl border border-red-200 shadow-sm">
                <h2 className="text-red-700 text-xl font-bold mb-2">Failed to Load</h2>
                <p className="text-red-600">{error || "This article cannot be displayed."}</p>
                <button
                    onClick={() => navigate(`/survey/${surveyId}`)}
                    className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition font-medium"
                >
                    Back to Survey
                </button>
            </div>
        );
    }

    const displayYear = article.publicationYear ?? article.year ?? "N/A";
    const displayAbstract = article.articleAbstract ?? article.abstractText ?? "No abstract available for this paper.";
    const displayId = article.externalId ?? article.id ?? "N/A";
    const displayOwnerName = article.addedByName ?? article.addedBy?.name ?? "Unknown";
    const displayOwnerRole = article.addedByRole ?? article.addedBy?.role ?? "Contributor";

    return (
        <div className="max-w-6xl mx-auto my-10 p-4 md:p-8">
            <Toaster position="top-right" />

            <button
                onClick={() => navigate(`/survey/${surveyId}`)}
                className="mb-6 inline-flex items-center text-sm font-medium text-indigo-600 hover:text-indigo-800 transition gap-1.5 group"
            >
                <span className="transform group-hover:-translate-x-0.5 transition-transform">←</span> Back to Survey Details
            </button>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

                {/* Left Panel */}
                <div className="lg:col-span-2 space-y-6 bg-white p-6 md:p-8 shadow-sm rounded-xl border border-gray-100">
                    <div className="border-b border-gray-100 pb-6">
                        <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${
                            article.status === 'INCLUDED' ? 'bg-green-100 text-green-800 border border-green-200' :
                                article.status === 'EXCLUDED' ? 'bg-red-100 text-red-800 border border-red-200' :
                                    'bg-yellow-100 text-yellow-800 border border-yellow-200'
                        }`}>
                            {article.status}
                        </span>

                        <h1 className="mt-3 text-2xl md:text-3xl font-extrabold text-gray-900 tracking-tight leading-tight">
                            {article.title}
                        </h1>

                        <div className="mt-4 flex flex-wrap gap-y-2 gap-x-4 text-sm text-gray-500">
                            <span className="flex items-center gap-1"><BookOpen className="w-4 h-4 text-gray-400" /> {article.journal}</span>
                            <span className="flex items-center gap-1"><Calendar className="w-4 h-4 text-gray-400" /> {displayYear}</span>
                        </div>
                    </div>

                    <div>
                        <h3 className="text-sm font-bold uppercase tracking-wider text-gray-400 mb-2">Authors</h3>
                        <div className="flex flex-wrap gap-2">
                            {article.authors && article.authors.length > 0 ? (
                                article.authors.map((author, index) => (
                                    <span key={index} className="bg-gray-50 text-gray-700 text-xs font-medium px-2.5 py-1 rounded-md border border-gray-200 flex items-center gap-1">
                                        <User className="w-3 h-3 text-gray-400" /> {author}
                                    </span>
                                ))
                            ) : (
                                <span className="text-gray-400 text-sm italic">No author data available.</span>
                            )}
                        </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-xl border border-slate-100 text-sm">
                        <div>
                            <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block mb-0.5">DOI Identifier</span>
                            <span className="font-mono font-medium text-gray-800 break-all">{article.doi || "N/A"}</span>
                        </div>
                        <div>
                            <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block mb-0.5">Original Resource</span>
                            {article.url ? (
                                <a href={article.url} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-indigo-600 hover:text-indigo-800 hover:underline font-semibold mt-0.5">
                                    Open External Paper <ExternalLink className="w-3.5 h-3.5" />
                                </a>
                            ) : <span className="text-gray-800">N/A</span>}
                        </div>
                    </div>

                    <div>
                        <h3 className="text-lg font-bold text-gray-900 border-b border-gray-100 pb-2">Abstract</h3>
                        <p className="mt-3 text-gray-700 text-sm leading-relaxed text-justify whitespace-pre-line">
                            {displayAbstract}
                        </p>
                    </div>

                    {article.inclusionSummary && (
                        <div className="p-4 bg-indigo-50/70 rounded-xl border border-indigo-100">
                            <h3 className="text-sm font-bold uppercase tracking-wider text-indigo-900 flex items-center gap-1.5">
                                <CheckCircle className="w-4 h-4 text-indigo-600" /> Inclusion Summary
                            </h3>
                            <p className="mt-2 text-sm text-indigo-900 leading-relaxed font-medium">
                                {article.inclusionSummary}
                            </p>
                        </div>
                    )}

                    <div className="pt-4 border-t border-gray-100 text-xs text-gray-400 flex flex-wrap justify-between gap-2">
                        <span>Added by: <strong className="text-gray-600">{displayOwnerName}</strong> ({displayOwnerRole})</span>
                        <span>Internal ID: <span className="font-mono">{displayId}</span></span>
                    </div>
                </div>

                {/* Right Panel */}
                <div className="space-y-4">
                    <div className="bg-gradient-to-br from-slate-900 to-indigo-950 text-white p-5 rounded-xl border border-slate-800 shadow-md flex items-center justify-between gap-4">
                        <div className="flex items-center gap-2">
                            <Sparkles className="w-5 h-5 text-indigo-400" />
                            <div>
                                <h2 className="font-bold text-md tracking-tight">Gemini Extractions</h2>
                            </div>
                        </div>
                        <button
                            onClick={generateTaxonomyWithGemma}
                            disabled={generating}
                            className="bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-800 text-white p-2 rounded-lg transition border border-indigo-400/20 shadow-sm group"
                            title="Run model classifications"
                        >
                            <RefreshCw className={`w-4 h-4 ${generating ? 'animate-spin' : 'group-hover:rotate-45'} transition-transform`} />
                        </button>
                    </div>

                    <div className="space-y-3 overflow-y-auto max-h-[750px] pr-1 custom-scrollbar">
                        {Object.keys(aiSuggestions).length === 0 ? (
                            <div className="bg-white p-8 rounded-xl border border-gray-200 text-center text-sm text-gray-400 shadow-sm flex flex-col items-center justify-center">
                                <HelpCircle className="w-8 h-8 text-gray-300 mb-2" />
                                <p className="mb-4">No parameters extracted yet.</p>
                                <button
                                    onClick={generateTaxonomyWithGemma}
                                    disabled={generating}
                                    className="px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-semibold text-xs border border-indigo-200 rounded-lg transition flex items-center gap-1.5"
                                >
                                    <Sparkles className="w-3.5 h-3.5" /> Analyze with Gemini
                                </button>
                            </div>
                        ) : (
                            Object.entries(aiSuggestions).map(([dimension, suggestion]) => {
                                const hasValues = suggestion.values && suggestion.values.length > 0;
                                const conf = suggestion.confidence?.toLowerCase();

                                return (
                                    <div key={dimension} className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm hover:border-gray-300 transition-colors">
                                        <div className="flex items-start justify-between gap-4 mb-2">
                                            <h4 className="text-xs font-bold text-gray-800 uppercase tracking-wide">{dimension}</h4>

                                            {suggestion.confidence && (
                                                <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                                                    conf === 'high' ? 'bg-green-50 text-green-700 border border-green-200' :
                                                        conf === 'medium' ? 'bg-amber-50 text-amber-700 border border-amber-200' :
                                                            'bg-red-50 text-red-700 border border-red-200'
                                                }`}>
                                                    {suggestion.confidence}
                                                </span>
                                            )}
                                        </div>

                                        <div className="flex flex-wrap gap-1.5 mb-3">
                                            {hasValues ? (
                                                suggestion.values?.map((v, i) => (
                                                    <span key={i} className="bg-indigo-50 text-indigo-700 text-xs font-semibold px-2.5 py-0.5 rounded-full border border-indigo-100">
                                                        {v}
                                                    </span>
                                                ))
                                            ) : (
                                                <span className="text-xs text-gray-400 italic">No values extracted</span>
                                            )}
                                        </div>

                                        {suggestion.proof && (
                                            <div className="bg-slate-50 border border-slate-100 rounded-lg p-2.5 relative group/quote">
                                                <div className="flex justify-between items-center mb-1">
                                                    <span className="text-[10px] uppercase font-bold text-gray-400 tracking-wider">Source Evidence</span>
                                                    <button
                                                        onClick={() => handleCopyText(suggestion.proof || '', dimension)}
                                                        className="text-gray-400 hover:text-indigo-600 opacity-0 group-hover/quote:opacity-100 transition-opacity p-0.5"
                                                    >
                                                        <Copy className="w-3 h-3" />
                                                    </button>
                                                </div>
                                                <p className="text-xs text-gray-600 italic leading-relaxed line-clamp-4 hover:line-clamp-none transition-all duration-200">
                                                    "{suggestion.proof}"
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>

            </div>
        </div>
    );
}