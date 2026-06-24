import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import { getArticle } from '../api/client'; // Adjust this relative path to match your folder structure

interface ArticleDto {
    externalId?: string;
    id?: string; // Fallback mapping identifier
    title: string;
    journal: string;
    publicationYear?: number;
    year?: number; // Fallback mapping matching your update request
    doi: string;
    url: string;
    status: string;
    articleAbstract?: string;
    abstractText?: string; // Fallback mapping matching database annotations
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
}

export default function ArticleDetails() {
    const { surveyId, articleId } = useParams<{ surveyId: string; articleId: string }>();
    const navigate = useNavigate();

    const [article, setArticle] = useState<ArticleDto | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        async function fetchArticleDetails() {
            try {
                setLoading(true);
                if (!articleId) throw new Error("Article ID is missing from the URL params.");

                // Using the centralized client request utility
                const data = await getArticle(articleId);
                setArticle(data as unknown as ArticleDto);
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

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
                <span className="ml-3 text-lg text-gray-600">Loading article details...</span>
            </div>
        );
    }

    if (error || !article) {
        return (
            <div className="max-w-4xl mx-auto mt-10 p-6 bg-red-50 rounded-lg border border-red-200">
                <h2 className="text-red-700 text-xl font-bold mb-2">Failed to Load</h2>
                <p className="text-red-600">{error || "This article cannot be displayed."}</p>
                <button
                    onClick={() => navigate(`/survey/${surveyId}`)}
                    className="mt-4 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition"
                >
                    Back to Survey
                </button>
            </div>
        );
    }

    // Normalized fallbacks to adapt safely to variation changes
    const displayYear = article.publicationYear ?? article.year ?? "N/A";
    const displayAbstract = article.articleAbstract ?? article.abstractText ?? "No abstract available for this paper.";
    const displayId = article.externalId ?? article.id ?? "N/A";
    const displayOwnerName = article.addedByName ?? article.addedBy?.name ?? "Unknown";
    const displayOwnerRole = article.addedByRole ?? article.addedBy?.role ?? "Contributor";

    return (
        <div className="max-w-5xl mx-auto my-10 p-8 bg-white shadow-md rounded-xl border border-gray-100">
            {/* Back Button */}
            <button
                onClick={() => navigate(`/survey/${surveyId}`)}
                className="mb-6 flex items-center text-sm font-medium text-indigo-600 hover:text-indigo-800 transition"
            >
                ← Back to Survey Details
            </button>

            {/* Title and Base Information */}
            <div className="border-b border-gray-200 pb-6">
                <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold ${
                    article.status === 'INCLUDED' ? 'bg-green-100 text-green-800' :
                        article.status === 'EXCLUDED' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800'
                }`}>
                    {article.status}
                </span>

                <h1 className="mt-3 text-3xl font-extrabold text-gray-900 tracking-tight">
                    {article.title}
                </h1>

                <p className="mt-2 text-md text-gray-500">
                    Published in: <span className="font-semibold text-gray-700">{article.journal}</span> ({displayYear})
                </p>
            </div>

            {/* Authors */}
            <div className="mt-6">
                <h3 className="text-lg font-bold text-gray-900">Authors:</h3>
                <div className="mt-2 flex flex-wrap gap-2">
                    {article.authors && article.authors.length > 0 ? (
                        article.authors.map((author, index) => (
                            <span key={index} className="bg-gray-100 text-gray-800 text-sm px-3 py-1 rounded-md border border-gray-200">
                                {author}
                            </span>
                        ))
                    ) : (
                        <span className="text-gray-500 text-sm italic">No author data available for this article.</span>
                    )}
                </div>
            </div>

            {/* Other Metadata (DOI / URL) */}
            <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4 bg-gray-50 p-4 rounded-lg border border-gray-100">
                <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-gray-400">DOI</span>
                    <p className="text-sm font-medium text-gray-800">{article.doi || "N/A"}</p>
                </div>
                <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-gray-400">Link to Original Paper</span>
                    <p className="text-sm">
                        {article.url ? (
                            <a href={article.url} target="_blank" rel="noreferrer" className="text-indigo-600 hover:underline font-medium">
                                Open External Link ↗
                            </a>
                        ) : "N/A"}
                    </p>
                </div>
            </div>

            {/* Abstract */}
            <div className="mt-8">
                <h3 className="text-xl font-bold text-gray-900 border-b pb-2">Abstract</h3>
                <p className="mt-3 text-gray-700 leading-relaxed text-justify whitespace-pre-line">
                    {displayAbstract}
                </p>
            </div>

            {/* Inclusion Summary */}
            {article.inclusionSummary && (
                <div className="mt-8 p-4 bg-indigo-50 rounded-lg border border-indigo-100">
                    <h3 className="text-lg font-bold text-indigo-900">Inclusion Summary</h3>
                    <p className="mt-2 text-sm text-indigo-900 leading-relaxed">
                        {article.inclusionSummary}
                    </p>
                </div>
            )}

            {/* Footer Attribution Data */}
            <div className="mt-8 pt-6 border-t border-gray-200 text-xs text-gray-400 flex justify-between">
                <span>Added by: <strong>{displayOwnerName}</strong> ({displayOwnerRole})</span>
                <span>Article ID: {displayId}</span>
            </div>
        </div>
    );
}