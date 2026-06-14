import { createBrowserRouter } from "react-router";
import SurveysListPage from "./pages/SurveysListPage";
import SurveyDetailsPage from "./pages/SurveyDetailsPage";
import EditArticlePage from "./pages/EditArticlePage";

export const router = createBrowserRouter([
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
  {
    path: "/survey/:surveyId/articles/:articleId/edit",
    Component: EditArticlePage,
  },
]);