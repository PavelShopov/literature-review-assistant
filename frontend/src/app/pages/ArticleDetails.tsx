import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import {
    Sparkles,
    ExternalLink,
    Calendar,
    BookOpen,
    User,
    RefreshCw,
    Save,
    CheckSquare,
    Square
} from 'lucide-react';
import { toast, Toaster } from 'sonner';
import { getArticle } from '../api/client'; // Adjust relative path as needed

interface LlmClassification {
    values: string[] | null;
    confidence: 'high' | 'medium' | 'low' | string | null;
    proof: string | null;
}

interface UserAnnotation {
    dimension: string;
    values: string[];
    confidence: 'high' | 'medium' | 'low' | 'N/A';
    proof: string;
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
    abstract?: string;
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

// Master Taxonomy Mapping Directory with explicit tracking string identifiers
const MASTER_TAXONOMY_DIMENSIONS = [
    {
        id: "methodology",
        name: "Research Methodology",
        options: ["Empirical Study", "Theoretical Analysis", "System Design", "Literature Review", "Case Study"]
    },
    {
        id: "dataset_context",
        name: "Target Dataset Context",
        options: ["Audio/Music", "Image Segmentation", "Text/NLP", "Synthetic Data", "Multi-modal"]
    },
    {
        id: "framework_approach",
        name: "Model / Framework Approach",
        options: ["Deep Learning (CNN/Transformer)", "Classical ML / Statistical", "Reinforcement Learning", "Rule-Based / Heuristic", "Hybrid System"]
    },
    {
        id: "evaluation_metrics",
        name: "Evaluation Metrics Utilized",
        options: ["Accuracy / F1-Score", "Loss / Perplexity", "Human Evaluation", "Throughput / Latency / Resource Cost", "Qualitative Analysis"]
    },
    {
        id: "data_sourcing",
        name: "Data Sourcing Type",
        options: ["Public Benchmark Dataset", "Proprietary / Private Data", "Scraped / Web-Harvested", "Synthetically Generated", "Not Applicable"]
    },
    {
        id: "research_focus",
        name: "Research Focus Area",
        options: ["Performance Optimization", "Security / Privacy / Robustness", "Explainability / Interpretability", "Novel Architecture Design", "Ethical / Bias Assessment"]
    },
    {
        id: "application_domain",
        name: "Primary Application Domain",
        options: ["Healthcare / Medicine", "Finance / Economics", "Autonomous Systems / Robotics", "E-commerce / Marketing", "General Purpose Tooling"]
    },
    {
        id: "computing_env",
        name: "Computing Environment",
        options: ["Cloud Infrastructure (AWS/GCP/Azure)", "On-Premises High-Performance Cluster", "Edge Devices / Internet of Things", "Local Workstation / Desktop", "Not Specified"]
    },
    {
        id: "open_science",
        name: "Code Availability & Open Science",
        options: ["Public Repository (GitHub/GitLab)", "Available Upon Request", "No Code Provided", "Commercial Software / Closed Source"]
    },
    {
        id: "learning_paradigm",
        name: "Learning Paradigm",
        options: ["Supervised Learning", "Unsupervised / Self-Supervised", "Semi-Supervised", "Few-Shot / Zero-Shot Learning", "Continual / Lifelong Learning"]
    },
    {
        id: "model_size",
        name: "Scale of Parameters / Model Size",
        options: ["Small (<10M parameters)", "Medium (10M - 1B parameters)", "Large / LLM Scale (>1B parameters)", "Non-Parametric Model", "Not Stated"]
    },
    {
        id: "hardware_reqs",
        name: "Hardware Requirements",
        options: ["Commodity CPU Only", "Single GPU Setup", "Multi-GPU / Distributed Cluster", "TPU / Specialized Accelerators", "Not Disclosed"]
    },
    {
        id: "limitations",
        name: "Limitations Acknowledged",
        options: ["Computational Cost Constraints", "Data Scarcity / Quality Issues", "Generalizability Concerns", "Ethical or Safety Risks", "No Formal Limitations Discussed"]
    },
    {
        id: "funding_source",
        name: "Funding Source Type",
        options: ["Government Grant (NSF/EU/etc.)", "Corporate / Industry Sponsored", "Academic Institutional Internal Funds", "Not Disclosed / Self-Funded"]
    },
    {
        id: "target_audience",
        name: "Target Audience / Stakeholder",
        options: ["Academic Researchers", "Industry Practitioners / Engineers", "End-Users / Consumers", "Policy Makers / Regulators"]
    }
];

// Fallback list of baseline IDs if user selection array is blank/missing
const DEFAULT_CRITERIA_IDS = ["methodology", "framework_approach", "evaluation_metrics"];

export default function ArticleDetails() {
    const { surveyId, articleId } = useParams<{ surveyId: string; articleId: string }>();
    const navigate = useNavigate();

    const [article, setArticle] = useState<ArticleDto | null>(null);
    const [aiSuggestions, setAiSuggestions] = useState<Record<string, LlmClassification>>({});
    const [loading, setLoading] = useState<boolean>(true);
    const [generating, setGenerating] = useState<boolean>(false);
    const [saving, setSaving] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const [activeCriteriaIds, setActiveCriteriaIds] = useState<string[]>([]);
    const [annotations, setAnnotations] = useState<Record<string, UserAnnotation>>({});

    // Filters down the displayed form areas
    const filteredTaxonomyDimensions = React.useMemo(() => {
        const targetingIds = activeCriteriaIds.length > 0 ? activeCriteriaIds : DEFAULT_CRITERIA_IDS;
        return MASTER_TAXONOMY_DIMENSIONS.filter(dim => targetingIds.includes(dim.id));
    }, [activeCriteriaIds]);



    useEffect(() => {
        async function fetchArticleDetails() {
            try {
                setLoading(true);
                if (!articleId) throw new Error("Article ID is missing from the URL params.");

                // 1. Fetch criteria dimensions from Database with LocalStorage and Preset Fallbacks
                let criteriaIds: string[] = [];

                if (surveyId) {
                    try {
                        // Extract numeric ID sequence in case url matches format "survey-123"
                        const parsedId = surveyId.includes('-') ? surveyId.split('-').pop() : surveyId;

                        const criteriaResponse = await fetch(`http://localhost:8080/api/surveys/${parsedId}/criteria`);
                        if (criteriaResponse.ok) {
                            const dbData = await criteriaResponse.json();
                            // Assumes DB payload layout returns an array directly, or an object containing a criteria field
                            const extractedIds = Array.isArray(dbData) ? dbData : dbData.criteria;

                            if (Array.isArray(extractedIds) && extractedIds.length > 0) {
                                criteriaIds = extractedIds;
                            }
                        }
                    } catch (dbFetchError) {
                        console.warn("Could not retrieve parameters from DB ecosystem. Checking localized caches...", dbFetchError);
                    }

                    // Cache fallback loop if database is unreachable or hasn't saved properties yet
                    if (criteriaIds.length === 0) {
                        const savedCriteria = localStorage.getItem(`survey:${surveyId}:criteria`);
                        if (savedCriteria) {
                            const parsed = JSON.parse(savedCriteria);
                            criteriaIds = Array.isArray(parsed) && parsed.length > 0 ? parsed : DEFAULT_CRITERIA_IDS;
                        } else {
                            criteriaIds = DEFAULT_CRITERIA_IDS;
                        }
                    }
                } else {
                    criteriaIds = DEFAULT_CRITERIA_IDS;
                }
                setActiveCriteriaIds(criteriaIds);

                // 2. Fetch foundational Article Metadata
                const data = (await getArticle(articleId)) as unknown as ArticleDto;
                setArticle(data);
                if (data.llm_classifications) {
                    setAiSuggestions(data.llm_classifications);
                }

                // 3. Populate empty validation state maps matching all schema elements
                const initialForm: Record<string, UserAnnotation> = {};
                MASTER_TAXONOMY_DIMENSIONS.forEach(dim => {
                    initialForm[dim.name] = {
                        dimension: dim.name,
                        values: [],
                        confidence: 'N/A',
                        proof: ''
                    };
                });

                // 4. Hydrate prior review records if they exist
                try {
                    const reviewResponse = await fetch(`http://localhost:8080/api/articles/${articleId}/review-data`);
                    if (reviewResponse.ok && reviewResponse.status !== 204) {
                        const savedReview = await reviewResponse.json();

                        if (savedReview && savedReview.jsonResponse) {
                            const parsedForm = JSON.parse(savedReview.jsonResponse) as Record<string, UserAnnotation>;

                            Object.keys(parsedForm).forEach(key => {
                                if (initialForm[key]) {
                                    initialForm[key] = {
                                        ...initialForm[key],
                                        values: parsedForm[key].values || [],
                                        confidence: parsedForm[key].confidence || 'N/A',
                                        proof: parsedForm[key].proof || ''
                                    };
                                }
                            });
                        }
                    }
                } catch (backendFetchErr) {
                    console.warn("No prior annotation records discovered for this session framework.", backendFetchErr);
                }

                setAnnotations(initialForm);

            } catch (err: any) {
                setError(err.message || "Something went wrong.");
            } finally {
                setLoading(false);
            }
        }

        if (articleId) {
            fetchArticleDetails();
        }
    }, [articleId, surveyId]);

    const generateTaxonomyWithGemma = async () => {
        if (!article || !articleId) return;
        try {
            setGenerating(true);
            toast.loading("Gemini is reading the paper details...", { id: "gemini-task" });

            const dynamicSchemaBlueprint = filteredTaxonomyDimensions.reduce((acc, dim) => {
                acc[dim.name] = {
                    values: dim.options,
                    confidence: "high | medium | low",
                    proof: "Verbatim quote string supporting your classification"
                };
                return acc;
            }, {} as Record<string, any>);

            const targetPrompt = `
You are a research paper analysis assistant. Analyze the following paper details:
Title: "${article.title}"
Abstract: "${article.abstract ?? ''}"

For each of the dimensions provided below, select relevant option values that map to the text. Provide a confidence level ('high', 'medium', 'low') and extract a short verbatim quote as evidence proof. You MUST respond with a valid JSON object ONLY.

The output format must strictly follow this structural schema layout matching the configuration constraints:
${JSON.stringify(dynamicSchemaBlueprint, null, 2)}
`.trim();

            const response = await fetch(`http://localhost:8080/api/articles/${articleId}/analyze-gemma`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ prompt: targetPrompt })
            });

            if (!response.ok) throw new Error(`Backend service returned error status: ${response.status}`);

            const payload = await response.json();
            setAiSuggestions(payload);
            toast.success("Taxonomy suggestions populated for selected dimensions!", { id: "gemini-task" });
        } catch (err: any) {
            console.error(err);
            toast.error(`Extraction Failed: ${err.message || err}`, { id: "gemini-task" });
        } finally {
            setGenerating(false);
        }
    };

    const toggleChipValue = (dimension: string, value: string) => {
        setAnnotations(prev => {
            const currentVals = prev[dimension]?.values || [];
            const nextVals = currentVals.includes(value)
                ? currentVals.filter(v => v !== value)
                : [...currentVals, value];
            return {
                ...prev,
                [dimension]: { ...prev[dimension], values: nextVals }
            };
        });
    };

    const handleSetConfidence = (dimension: string, conf: 'high' | 'medium' | 'low' | 'N/A') => {
        setAnnotations(prev => ({
            ...prev,
            [dimension]: { ...prev[dimension], confidence: conf }
        }));
    };

    const handleSetProof = (dimension: string, text: string) => {
        setAnnotations(prev => ({
            ...prev,
            [dimension]: { ...prev[dimension], proof: text }
        }));
    };

    const handleCopyAiToForm = (dimensionName: string) => {
        const aiSuggestion = aiSuggestions[dimensionName];
        if (!aiSuggestion) {
            toast.error("No AI baseline data found for this dimension.");
            return;
        }

        setAnnotations(prev => ({
            ...prev,
            [dimensionName]: {
                dimension: dimensionName,
                values: aiSuggestion.values || [],
                confidence: (aiSuggestion.confidence?.toLowerCase() === 'high' ||
                    aiSuggestion.confidence?.toLowerCase() === 'medium' ||
                    aiSuggestion.confidence?.toLowerCase() === 'low')
                    ? (aiSuggestion.confidence.toLowerCase() as any)
                    : 'N/A',
                proof: aiSuggestion.proof || ''
            }
        }));
        toast.success(`Injected suggestions into your "${dimensionName}" answer!`);
    };

    const handleSaveReviewForm = async () => {
        try {
            setSaving(true);
            const response = await fetch(`http://localhost:8080/api/articles/${articleId}/review`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ reviewForm: annotations })
            });

            if (!response.ok) {
                const errorText = await response.text().catch(() => "Unknown server error");
                throw new Error(`Server returned status ${response.status}: ${errorText}`);
            }
            toast.success("Review assessments saved successfully!");
        } catch (err: any) {
            toast.error(`Error saving review: ${err.message}`);
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen bg-gray-50">
                <div className="flex flex-col items-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
                    <span className="ml-3 mt-4 text-lg text-gray-600 font-medium">Loading review workbench...</span>
                </div>
            </div>
        );
    }

    if (error || !article) {
        return (
            <div className="max-w-4xl mx-auto mt-10 p-6 bg-red-50 rounded-xl border border-red-200 shadow-sm">
                <h2 className="text-red-700 text-xl font-bold mb-2">Failed to Load</h2>
                <p className="text-red-600">{error || "This article cannot be displayed."}</p>
                <button onClick={() => navigate(`/survey/${surveyId}`)} className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition font-medium">
                    Back to Survey
                </button>
            </div>
        );
    }

    const displayYear = article.publicationYear ?? article.year ?? "N/A";
    const displayAbstract = article.abstract ?? "No abstract available for this paper.";

    return (
        <div className="max-w-7xl mx-auto my-6 px-4 md:px-8 space-y-6">
            <Toaster position="top-right" />

            {/* Navigation Row */}
            <div className="flex justify-between items-center">
                <button
                    onClick={() => navigate(`/survey/${surveyId}`)}
                    className="inline-flex items-center text-sm font-medium text-indigo-600 hover:text-indigo-800 transition gap-1.5 group"
                >
                    <svg
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        strokeWidth={2}
                        stroke="currentColor"
                        className="w-4 h-4 transform transition-transform group-hover:-translate-x-0.5"
                    >
                        <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18"/>
                    </svg>
                    Back to Survey Workspace
                </button>

                <button
                    onClick={handleSaveReviewForm}
                    disabled={saving}
                    className="inline-flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white font-semibold text-sm px-5 py-2 rounded-xl shadow-sm transition disabled:bg-gray-300"
                >
                    <Save className="w-4 h-4" /> {saving ? "Saving Changes..." : "Submit Review Form"}
                </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">

                {/* Paper Information Metadata Layout */}
                <div className="lg:col-span-5 space-y-6 bg-white p-6 shadow-sm rounded-xl border border-gray-200">
                    <div className="border-b border-gray-100 pb-4">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                            article.status === 'INCLUDED' ? 'bg-green-100 text-green-800 border border-green-200' :
                                article.status === 'EXCLUDED' ? 'bg-red-100 text-red-800 border border-red-200' :
                                    'bg-yellow-100 text-yellow-800 border border-yellow-200'
                        }`}>
                          {article.status}
                        </span>
                        <h1 className="mt-2 text-xl font-extrabold text-gray-900 leading-snug">{article.title}</h1>
                        <p className="mt-2 text-xs text-gray-400 flex items-center gap-3">
                            <span className="flex items-center gap-1"><BookOpen className="w-3.5 h-3.5" /> {article.journal}</span>
                            <span className="flex items-center gap-1"><Calendar className="w-3.5 h-3.5" /> {displayYear}</span>
                        </p>

                        {article.doi && (
                            <a
                                href={`https://doi.org/${article.doi}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="mt-4 inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition"
                            >
                                <ExternalLink className="w-4 h-4" />
                                View Article
                            </a>
                        )}
                    </div>

                    <div>
                        <span className="text-[10px] font-bold uppercase tracking-wide text-gray-400 block mb-2">Authors</span>
                        <div className="flex flex-wrap gap-1.5">
                            {article.authors?.map((a, i) => (
                                <span key={i} className="bg-gray-50 text-gray-600 text-[11px] px-2 py-0.5 rounded border border-gray-200 inline-flex items-center gap-1">
                                    <User className="w-2.5 h-2.5 text-gray-400" /> {a}
                                </span>
                            ))}
                        </div>
                    </div>

                    <div>
                        <span className="text-xs font-bold text-gray-900 block border-b pb-1 mb-2">Abstract Profile</span>
                        <p className="text-xs text-gray-600 leading-relaxed text-justify max-h-64 overflow-y-auto pr-1">
                            {displayAbstract}
                        </p>
                    </div>

                    {article.inclusionSummary && (
                        <div className="p-3 bg-indigo-50/50 rounded-lg border border-indigo-100 text-xs">
                            <span className="font-bold text-indigo-900 block mb-1">Inclusion Rationale</span>
                            <p className="text-indigo-950 italic">"{article.inclusionSummary}"</p>
                        </div>
                    )}
                </div>

                {/* Screening Assessment Questionnaire Form Workspaces */}
                <div className="lg:col-span-7 space-y-6">

                    {/* Review Assistant Pipeline Trigger Header */}
                    <div className="bg-slate-900 text-white p-4 rounded-xl border border-slate-800 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <Sparkles className="w-4 h-4 text-indigo-400" />
                            <div>
                                <h3 className="font-bold text-sm">Review Assistant Pipeline</h3>
                                <p className="text-[11px] text-slate-400">Leverage AI generation values to assist your taxonomy answer</p>
                            </div>
                        </div>
                        <button
                            onClick={generateTaxonomyWithGemma}
                            disabled={generating}
                            className="bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-800 px-3 py-1.5 text-xs font-semibold text-white rounded-lg transition inline-flex items-center gap-1.5"
                        >
                            <RefreshCw className={`w-3.5 h-3.5 ${generating ? 'animate-spin' : ''}`} /> Run Gemini Extraction
                        </button>
                    </div>

                    {/* Screening Form Loops */}
                    {filteredTaxonomyDimensions.map((dim) => {
                        const currentAnn = annotations[dim.name] || { values: [], confidence: 'N/A', proof: '' };
                        const aiData = aiSuggestions[dim.name];

                        return (
                            <div key={dim.name} className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm space-y-4">
                                <div className="border-b border-gray-100 pb-2">
                                    <h3 className="text-sm font-bold text-gray-800 tracking-wide uppercase">{dim.name}</h3>
                                </div>

                                {/* Question Value Choice Selection Grid */}
                                <div>
                                    <label className="text-xs font-semibold text-gray-400 block mb-2">Select Values mapped in this text:</label>
                                    <div className="flex flex-wrap gap-2">
                                        {dim.options.map(opt => {
                                            const isSelected = currentAnn.values.includes(opt);
                                            return (
                                                <button
                                                    key={opt}
                                                    onClick={() => toggleChipValue(dim.name, opt)}
                                                    className={`px-3 py-1 text-xs font-medium rounded-full border transition flex items-center gap-1.5 ${
                                                        isSelected
                                                            ? 'bg-indigo-600 border-indigo-600 text-white shadow-sm'
                                                            : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'
                                                    }`}
                                                >
                                                    {isSelected ? <CheckSquare className="w-3 h-3" /> : <Square className="w-3 h-3" />}
                                                    {opt}
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>

                                {/* Form Row 2: Confidence level selectors and verbatim notes */}
                                <div className="grid grid-cols-1 md:grid-cols-12 gap-4 pt-2">

                                    {/* Confidence Buttons */}
                                    <div className="md:col-span-5 space-y-2">
                                        <label className="text-xs font-semibold text-gray-400 block">Reviewer Confidence</label>
                                        <div className="grid grid-cols-2 gap-1.5 text-xs font-medium">
                                            {(['high', 'medium', 'low'] as const).map(c => (
                                                <button
                                                    key={c}
                                                    onClick={() => handleSetConfidence(dim.name, c)}
                                                    className={`py-1.5 px-2 rounded-lg border text-center capitalize transition ${
                                                        currentAnn.confidence === c
                                                            ? c === 'high' ? 'bg-green-50 border-green-500 text-green-700 font-bold' :
                                                                c === 'medium' ? 'bg-amber-50 border-amber-500 text-amber-700 font-bold' :
                                                                    'bg-red-50 border-red-500 text-red-700 font-bold'
                                                            : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'
                                                    }`}
                                                >
                                                    {c}
                                                </button>
                                            ))}
                                            <button
                                                onClick={() => handleSetConfidence(dim.name, 'N/A')}
                                                className={`py-1.5 px-2 rounded-lg border text-center transition ${
                                                    currentAnn.confidence === 'N/A'
                                                        ? 'bg-gray-100 border-gray-400 text-gray-700 font-bold'
                                                        : 'bg-white border-gray-200 text-gray-500 hover:bg-gray-50'
                                                }`}
                                            >
                                                N/A
                                            </button>
                                        </div>
                                    </div>

                                    {/* Verbatim Proof Area */}
                                    <div className="md:col-span-7 space-y-1">
                                        <label className="text-xs font-semibold text-gray-400 block">Proof / Verbatim Quote Evidence</label>
                                        <textarea
                                            value={currentAnn.proof}
                                            onChange={(e) => handleSetProof(dim.name, e.target.value)}
                                            placeholder="Paste text snippets or extraction sentences confirming your taxonomy selection..."
                                            className="w-full text-xs p-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-1 focus:ring-indigo-500 min-h-[76px] placeholder-gray-300 shadow-inner"
                                        />
                                    </div>
                                </div>

                                {/* AI Suggestions Integration Component */}
                                {aiData && (
                                    <div className="mt-4 bg-indigo-50/50 rounded-xl border border-indigo-100/70 p-3 space-y-2 text-xs">
                                        <div className="flex justify-between items-center">
                                            <span className="flex items-center gap-1 font-bold text-indigo-900">
                                                <Sparkles className="w-3.5 h-3.5 text-indigo-500" /> AI Suggestions extraction
                                            </span>
                                            <button
                                                onClick={() => handleCopyAiToForm(dim.name)}
                                                className="text-[11px] font-semibold text-indigo-700 hover:text-indigo-900 bg-white px-2 py-0.5 rounded border border-indigo-200 shadow-xs transition"
                                            >
                                                Copy to my answer
                                            </button>
                                        </div>

                                        <div className="flex flex-wrap items-center gap-1.5">
                                            <span className="text-gray-400 font-medium">Extracted values:</span>
                                            {aiData.values && aiData.values.length > 0 ? (
                                                aiData.values.map((v, i) => (
                                                    <span key={i} className="bg-white text-indigo-800 px-2 py-0.5 rounded font-mono border text-[10px]">
                                                        {v}
                                                    </span>
                                                ))
                                            ) : (
                                                <span className="text-gray-400 italic text-[11px]">No categorical labels found</span>
                                            )}

                                            {aiData.confidence && (
                                                <span className={`ml-auto px-1.5 py-0.2 rounded font-bold uppercase text-[9px] ${
                                                    aiData.confidence.toLowerCase() === 'high' ? 'bg-green-100 text-green-700' :
                                                        aiData.confidence.toLowerCase() === 'medium' ? 'bg-amber-100 text-amber-700' :
                                                            'bg-red-100 text-red-700'
                                                }`}>
                                                    {aiData.confidence}
                                                </span>
                                            )}
                                        </div>

                                        {aiData.proof && (
                                            <div className="bg-white/80 p-2 rounded border border-indigo-50 text-gray-600 italic leading-normal">
                                                "{aiData.proof}"
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>

            </div>
        </div>
    );
}
