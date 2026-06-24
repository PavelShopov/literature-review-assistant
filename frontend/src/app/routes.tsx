import { createBrowserRouter } from "react-router";
import SurveysListPage from "./pages/SurveysListPage";
import SurveysToReviewPage from "./pages/SurveysToReviewPage";
import SurveyDetailsPage from "./pages/SurveyDetailsPage";
import EditArticlePage from "./pages/EditArticlePage";
import ArticleDetailsPage from "./pages/ArticleDetails";
import AuthPage from "./pages/AuthPage";
import ReviewSurveyPage from "./pages/ReviewSurveyPage";
import { RequireAuth } from "./auth/RequireAuth";

export const router = createBrowserRouter([
  {
    path: "/login",
    element: <AuthPage mode="login" />,
  },
  {
    path: "/register",
    element: <AuthPage mode="register" />,
  },
  {
    element: <RequireAuth />,
    children: [
      {
        path: "/",
        Component: SurveysListPage,
      },
      {
        path: "/survey",
        Component: SurveysListPage,
      },
      {
        path: "/surveys/to-review",
        Component: SurveysToReviewPage,
      },
      {
        path: "/surveys/to-review/:surveyId/review",
        Component: ReviewSurveyPage,
      },
      {
        path: "/survey/:surveyId",
        Component: SurveyDetailsPage,
      },
      {
        path: "/survey/:surveyId/articles",
        Component: SurveyDetailsPage,
      },
      // Fixed: Appended /view to match your SurveyDetailsPage handleView navigation precisely
      {
        path: "/survey/:surveyId/articles/:articleId/view",
        Component: ArticleDetailsPage,
      },
      // Optional Fallback: Just in case you ever navigate without /view elsewhere
      {
        path: "/survey/:surveyId/articles/:articleId",
        Component: ArticleDetailsPage,
      },
      {
        path: "/survey/:surveyId/articles/:articleId/edit",
        Component: EditArticlePage,
      },
    ],
  },
]);