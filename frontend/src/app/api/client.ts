import type { Article } from "../components/ArticleCard";
import type { Contributor } from "../components/ContributorsPanel";
import type { Survey } from "../pages/SurveysListPage";
import {
  mockArticles,
  mockContributors,
  mockSurveyDetails,
  mockSurveys,
  type SurveyDetails,
} from "./mockData";

export { mockArticles, type SurveyDetails };

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
export const isMockApi = import.meta.env.VITE_USE_MOCK_API !== "false";
const MOCK_SURVEYS_KEY = "mock:surveys";
const MOCK_USERS_KEY = "mock:users";

export type AuthUser = {
  id: string | number;
  name: string;
  email: string;
};

export type AuthResponse = {
  token: string;
  user: AuthUser;
};

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value));

const mockDelay = async <T>(value: T): Promise<T> => {
  await new Promise((resolve) => setTimeout(resolve, 250));
  return clone(value);
};

const getMockSurveys = (): Survey[] => {
  const stored = localStorage.getItem(MOCK_SURVEYS_KEY);
  if (!stored) return mockSurveys;

  try {
    const surveys = JSON.parse(stored) as Survey[];
    return surveys.length > 0 ? surveys : mockSurveys;
  } catch {
    return mockSurveys;
  }
};

const setMockSurveys = (surveys: Survey[]) => {
  localStorage.setItem(MOCK_SURVEYS_KEY, JSON.stringify(surveys));
};

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, options);

  if (!response.ok) {
    const errorBody = await response.json().catch(() => undefined);
    throw new Error(errorBody?.message ?? errorBody?.detail ?? `API request failed: ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

const getMockUsers = (): Array<AuthUser & { password: string }> => {
  const stored = localStorage.getItem(MOCK_USERS_KEY);
  if (!stored) {
    return [];
  }

  try {
    return JSON.parse(stored);
  } catch {
    return [];
  }
};

const setMockUsers = (users: Array<AuthUser & { password: string }>) => {
  localStorage.setItem(MOCK_USERS_KEY, JSON.stringify(users));
};

export function registerUser(input: { name: string; email: string; password: string }): Promise<AuthResponse> {
  if (isMockApi) {
    const email = input.email.trim().toLowerCase();
    const users = getMockUsers();
    if (users.some((user) => user.email === email)) {
      return Promise.reject(new Error("An account with this email already exists"));
    }

    const user = {
      id: `user-${Date.now()}`,
      name: input.name.trim(),
      email,
      password: input.password,
    };

    setMockUsers([...users, user]);
    return mockDelay({
      token: `mock-token-${user.id}`,
      user: { id: user.id, name: user.name, email: user.email },
    });
  }

  return request<AuthResponse>("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function loginUser(input: { email: string; password: string }): Promise<AuthResponse> {
  if (isMockApi) {
    const email = input.email.trim().toLowerCase();
    const user = getMockUsers().find((item) => item.email === email && item.password === input.password);
    if (!user) {
      return Promise.reject(new Error("Invalid email or password"));
    }

    return mockDelay({
      token: `mock-token-${user.id}`,
      user: { id: user.id, name: user.name, email: user.email },
    });
  }

  return request<AuthResponse>("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function logoutUser(token: string): Promise<void> {
  if (isMockApi) return mockDelay(undefined);

  return request<void>("/api/auth/logout", {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function getSurveys(): Promise<Survey[]> {
  if (isMockApi) return mockDelay(getMockSurveys());
  return request<Survey[]>("/api/surveys");
}

export function getSurvey(surveyId: string): Promise<SurveyDetails> {
  if (isMockApi) {
    const seededDetails = mockSurveyDetails[surveyId];
    if (seededDetails) return mockDelay(seededDetails);

    const survey = getMockSurveys().find((item) => item.id === surveyId);
    if (survey) {
      return mockDelay({
        ...survey,
        researchQuestion: "Define the main research question for this survey.",
        screened: 0,
        pending: survey.totalArticles,
        owner: {
          name: "Survey Owner",
          email: "owner@example.com",
        },
      });
    }

    return mockDelay({
      ...mockSurveyDetails["survey-001"],
      id: surveyId,
    });
  }

  return request<SurveyDetails>(`/api/surveys/${surveyId}`);
}

export function saveSurvey(survey: Survey): Promise<Survey> {
  if (isMockApi) {
    const surveys = getMockSurveys();
    const exists = surveys.some((item) => item.id === survey.id);
    const nextSurveys = exists
      ? surveys.map((item) => (item.id === survey.id ? survey : item))
      : [survey, ...surveys];

    setMockSurveys(nextSurveys);
    return mockDelay(survey);
  }

  return request<Survey>(`/api/surveys/${survey.id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(survey),
  });
}

export function deleteSurvey(surveyId: string): Promise<void> {
  if (isMockApi) {
    setMockSurveys(getMockSurveys().filter((item) => item.id !== surveyId));
    return mockDelay(undefined);
  }

  return request<void>(`/api/surveys/${surveyId}`, {
    method: "DELETE",
  });
}

export function getArticle(articleId: string): Promise<Article> {
  if (isMockApi) {
    const article = mockArticles.find((item) => item.id === articleId) ?? mockArticles[0];
    return mockDelay(article);
  }

  return request<Article>(`/api/surveys/articles/${articleId}`);
}

export function getContributors(surveyId: string): Promise<Contributor[]> {
  if (isMockApi) {
    const storageKey = `survey:${surveyId}:contributors`;
    const stored = localStorage.getItem(storageKey);
    if (stored) {
      const storedContributors = JSON.parse(stored) as Contributor[];
      const hasReviewer = storedContributors.some((item) => item.role !== "Owner");
      return mockDelay(hasReviewer ? storedContributors : mockContributors);
    }

    return mockDelay(mockContributors);
  }

  return request<Contributor[]>(`/api/surveys/${surveyId}/contributors`);
}

export function addContributor(surveyId: string, contributor: Contributor): Promise<Contributor> {
  if (isMockApi) return mockDelay(contributor);

  return request<Contributor>(`/api/surveys/${surveyId}/contributors`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(contributor),
  });
}

export function removeContributor(surveyId: string, contributorId: string): Promise<void> {
  if (isMockApi) return mockDelay(undefined);

  return request<void>(`/api/surveys/${surveyId}/contributors/${contributorId}`, {
    method: "DELETE",
  });
}
