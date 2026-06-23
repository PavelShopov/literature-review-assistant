import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router';

interface ArticleDto {
    externalId: string;
    title: string;
    journal: string;
    publicationYear: number;
    doi: string;
    url: string;
    status: string;
    articleAbstract: string;
    inclusionSummary: string;
    addedById: string;
    addedByName: string;
    addedByRole: string;
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
                const response = await fetch(`http://localhost:8080/api/surveys/${surveyId}/articles/${articleId}`);

                if (!response.ok) {
                    if (response.status === 404) {
                        throw new Error("Артиклот не е пронајден во базата на податоци.");
                    }
                    throw new Error("Се случи грешка при преземање на податоците.");
                }

                const data = await response.json();
                setArticle(data);
            } catch (err: any) {
                setError(err.message || "Нешто тргна наопаку.");
            } finally {
                setLoading(false);
            }
        }

        if (surveyId && articleId) {
            fetchArticleDetails();
        }
    }, [surveyId, articleId]);

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
                <span className="ml-3 text-lg text-gray-600">Се вчитуваат деталите за артиклот...</span>
            </div>
        );
    }

    if (error || !article) {
        return (
            <div className="max-w-4xl mx-auto mt-10 p-6 bg-red-50 rounded-lg border border-red-200">
                <h2 className="text-red-700 text-xl font-bold mb-2">Грешка при вчитување</h2>
                <p className="text-red-600">{error || "Артиклот не може да се прикаже."}</p>
                <button
                    onClick={() => navigate(`/surveys/${surveyId}`)}
                    className="mt-4 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition"
                >
                    Назад кон анкетата
                </button>
            </div>
        );
    }

    return (
        <div className="max-w-5xl mx-auto my-10 p-8 bg-white shadow-md rounded-xl border border-gray-100">
            {/* Копче за назад */}
            <button
                onClick={() => navigate(`/surveys/${surveyId}`)}
                className="mb-6 flex items-center text-sm font-medium text-indigo-600 hover:text-indigo-800 transition"
            >
                ← Назад кон детали за анкетата
            </button>

            {/* Наслов и Основни информации */}
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
                    Објавено во: <span className="font-semibold text-gray-700">{article.journal}</span> ({article.publicationYear})
                </p>
            </div>

            {/* Автори */}
            <div className="mt-6">
                <h3 className="text-lg font-bold text-gray-900">Автори:</h3>
                <div className="mt-2 flex flex-wrap gap-2">
                    {article.authors && article.authors.length > 0 ? (
                        article.authors.map((author, index) => (
                            <span key={index} className="bg-gray-100 text-gray-800 text-sm px-3 py-1 rounded-md border border-gray-200">
                {author}
              </span>
                        ))
                    ) : (
                        <span className="text-gray-500 text-sm italic">Нема зачувано автори за овој артикл.</span>
                    )}
                </div>
            </div>

            {/* Останати метаподатоци (DOI / URL) */}
            <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4 bg-gray-50 p-4 rounded-lg border border-gray-100">
                <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-gray-400">DOI</span>
                    <p className="text-sm font-medium text-gray-800">{article.doi || "N/A"}</p>
                </div>
                <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-gray-400">Линк до оригинален труд</span>
                    <p className="text-sm">
                        {article.url ? (
                            <a href={article.url} target="_blank" rel="noreferrer" className="text-indigo-600 hover:underline font-medium">
                                Отвори надворешна врска ↗
                            </a>
                        ) : "N/A"}
                    </p>
                </div>
            </div>

            {/* Апстракт */}
            <div className="mt-8">
                <h3 className="text-xl font-bold text-gray-900 border-b pb-2">Abstract</h3>
                <p className="mt-3 text-gray-700 leading-relaxed text-justify whitespace-pre-line">
                    {article.articleAbstract || "Нема достапен апстракт за овој труд."}
                </p>
            </div>

            {/* Резиме за инклузија (Доколку го има) */}
            {article.inclusionSummary && (
                <div className="mt-8 p-4 bg-indigo-50 rounded-lg border border-indigo-100">
                    <h3 className="text-lg font-bold text-indigo-900">Образложение за статус (Inclusion Summary)</h3>
                    <p className="mt-2 text-sm text-indigo-900 leading-relaxed">
                        {article.inclusionSummary}
                    </p>
                </div>
            )}

            {/* Информации за тој што го додал артиклот */}
            <div className="mt-8 pt-6 border-t border-gray-200 text-xs text-gray-400 flex justify-between">
                <span>Додадено од: <strong>{article.addedByName}</strong> ({article.addedByRole})</span>
                <span>ID на труд: {article.externalId}</span>
            </div>
        </div>
    );
}