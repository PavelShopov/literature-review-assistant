import { createBrowserRouter } from "react-router";
import SurveysListPage from "./pages/SurveysListPage";
import SurveyDetailsPage from "./pages/SurveyDetailsPage";
import EditArticlePage from "./pages/EditArticlePage";
import ArticleDetailsPage from "./pages/ArticleDetails";
import AuthPage from "./pages/AuthPage";
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
        path: "/survey/:surveyId",
        Component: SurveyDetailsPage,
      },
      {
        path: "/survey/:surveyId/articles",
        Component: SurveyDetailsPage,
      },
      //
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