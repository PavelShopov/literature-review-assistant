# Frontend README

This React/Vite frontend can run in two modes:

- `mock` mode: no backend required. Data is read from `src/app/api/mockData.ts` and browser `localStorage`.
- `backend` mode: calls the real Spring Boot backend at `http://localhost:8080`.

## Run

```powershell
npm install
npm run dev
```

`npm run dev` uses mock mode by default.

```powershell
npm run dev:backend
```

`npm run dev:backend` disables mocks and calls the backend configured in `.env.backend`.

## Environment Files

`.env.development`

```env
VITE_USE_MOCK_API=true
```

`.env.mock`

```env
VITE_USE_MOCK_API=true
```

`.env.backend`

```env
VITE_USE_MOCK_API=false
VITE_API_BASE_URL=http://localhost:8080
```

The API switch is in `src/app/api/client.ts`:

```ts
export const isMockApi = import.meta.env.VITE_USE_MOCK_API !== "false";
```

So anything except `VITE_USE_MOCK_API=false` runs mocked.

## Routes

| Frontend route | Component | Notes |
| --- | --- | --- |
| `/login` | `AuthPage` | Login form |
| `/register` | `AuthPage` | Register form |
| `/` | `SurveysListPage` | Protected, redirects to `/login` if no session |
| `/survey` | `SurveysListPage` | Survey list |
| `/survey/:surveyId` | `SurveyDetailsPage` | Dashboard/articles/Ask AI/reviewers |
| `/survey/:surveyId/articles` | `SurveyDetailsPage` | Same page, articles tab handled in component state |
| `/survey/:surveyId/articles/:articleId/edit` | `EditArticlePage` | Article edit form |

Auth route protection is in:

- `src/app/auth/AuthContext.tsx`
- `src/app/auth/RequireAuth.tsx`

## Mock Storage

Mock mode stores user-created data in browser `localStorage`.

| Key | What it stores |
| --- | --- |
| `auth:session` | Current logged-in user and token |
| `mock:users` | Mock registered users |
| `mock:surveys` | Mock created/edited surveys |
| `survey:{surveyId}:contributors` | Reviewers for a survey |
| `survey:{surveyId}:reference-decisions` | Ask AI reference decisions |

To reset mock data, clear browser site data or remove these keys from DevTools.

## API Mapping

All API calls are centralized in `src/app/api/client.ts`.

### Auth

| Frontend function | Backend endpoint | Request | Expected response |
| --- | --- | --- | --- |
| `registerUser` | `POST /api/auth/register` | `{ name, email, password }` | `{ token, user }` |
| `loginUser` | `POST /api/auth/login` | `{ email, password }` | `{ token, user }` |
| `logoutUser` | `DELETE /api/auth/logout` | `Authorization: Bearer <token>` | `204 No Content` |

Expected auth response:

```ts
type AuthResponse = {
  token: string;
  user: {
    id: string | number;
    name: string;
    email: string;
  };
};
```

### Surveys

| Frontend function | Backend endpoint | Notes |
| --- | --- | --- |
| `getSurveys` | `GET /api/surveys` | Survey list |
| `getSurvey` | `GET /api/surveys/{surveyId}` | Survey details |
| `saveSurvey` | `PUT /api/surveys/{surveyId}` | Used for create/update in frontend right now |
| `deleteSurvey` | `DELETE /api/surveys/{surveyId}` | Delete survey |

Survey list item shape:

```ts
type Survey = {
  id: string;
  name: string;
  description: string;
  createdDate: string;
  status: "In Progress" | "Completed" | "Draft";
  totalArticles: number;
};
```

Survey details shape:

```ts
type SurveyDetails = Survey & {
  researchQuestion: string;
  screened: number;
  pending: number;
  owner?: {
    name: string;
    email: string;
  };
};
```

Backend note: if the backend uses `surveyId` and `title` internally, map them to frontend `id` and `name`, or adjust the frontend DTOs.

### Articles

| Frontend function | Backend endpoint | Notes |
| --- | --- | --- |
| `getArticle` | `GET /api/surveys/articles/{articleId}` | Used by edit article page |

Article shape:

```ts
type Article = {
  id: string;
  title: string;
  authors: string[];
  journal: string;
  year: number;
  doi: string;
  status: "INCLUDED" | "EXCLUDED" | "PENDING";
  abstract?: string;
  inclusionSummary?: string;
  addedBy?: {
    id: string;
    name: string;
    role: "Owner" | "Reviewer";
  };
};
```

Reviewer permissions in the UI depend on `addedBy.id`.

### Reviewers

The UI label is **Reviewers**. Some internal function names still say `Contributor` because they were built before the copy change.

| Frontend function | Backend endpoint | Notes |
| --- | --- | --- |
| `getContributors` | `GET /api/surveys/{surveyId}/contributors` | Should return reviewers/owner |
| `addContributor` | `POST /api/surveys/{surveyId}/contributors` | Adds reviewer |
| `removeContributor` | `DELETE /api/surveys/{surveyId}/contributors/{contributorId}` | Removes reviewer |

Expected reviewer shape:

```ts
type Contributor = {
  id: string;
  name: string;
  email: string;
  role: "Owner" | "Reviewer";
  addedDate: string;
};
```

Backend can keep endpoint names as `contributors` for now, but returned data should use role `Reviewer`, not `Contributor`.

## Owner vs Reviewer View

Survey details supports a temporary role switch for UI testing:

```text
/survey/survey-001?role=owner
/survey/survey-001?role=reviewer
```

Owner:

- can manage reviewers
- can edit/remove all articles

Reviewer:

- can add/import articles
- can edit/remove only articles they added

This is currently frontend-enforced for preview. Backend should eventually enforce the same permissions.

## Backend Auth Notes

The real backend auth implementation is in:

- `src/main/java/.../config/SecurityConfig.java`
- `src/main/java/.../model/AppUser.java`
- `src/main/java/.../model/AuthSession.java`
- `src/main/java/.../repository/AppUserRepository.java`
- `src/main/java/.../repository/AuthSessionRepository.java`
- `src/main/java/.../service/AuthService.java`
- `src/main/java/.../service/impl/AuthServiceImpl.java`
- `src/main/java/.../web/controller/AuthController.java`
- `src/main/java/.../web/dto/*`

When running `npm run dev:backend`, Spring Boot must also be running on port `8080`.

## Known Integration Gaps

- Survey create/update/delete endpoints may need backend implementation if they are currently mock-only or hard-coded.
- Article import/delete/status updates are mostly local UI state right now.
- Reviewer permissions are currently frontend-only.
- Ask AI reference decisions are stored in localStorage in mock mode.
