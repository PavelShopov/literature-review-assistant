import type { Contributor } from "../components/ContributorsPanel";
import type { Article } from "../components/ArticleCard";
import type { Survey } from "../pages/SurveysListPage";

export type SurveyDetails = Survey & {
  researchQuestion: string;
  screened: number;
  pending: number;
  owner?: {
    name: string;
    email: string;
  };
};

export const mockSurveys: Survey[] = [
  {
    id: "survey-001",
    name: "Impact of AI on Healthcare Outcomes",
    description:
      "A systematic literature review examining the effectiveness and implementation of artificial intelligence technologies in healthcare settings.",
    createdDate: "2024-03-15",
    status: "In Progress",
    totalArticles: 6,
  },
  {
    id: "survey-002",
    name: "Climate Change and Agricultural Productivity",
    description:
      "Analyzing the relationship between climate change factors and crop yields across different geographic regions.",
    createdDate: "2024-02-20",
    status: "In Progress",
    totalArticles: 12,
  },
  {
    id: "survey-003",
    name: "Remote Work and Employee Wellbeing",
    description:
      "Investigating the psychological and productivity impacts of remote work arrangements post-pandemic.",
    createdDate: "2024-01-10",
    status: "Completed",
    totalArticles: 8,
  },
];

export const mockSurveyDetails: Record<string, SurveyDetails> = {
  "survey-001": {
    ...mockSurveys[0],
    researchQuestion:
      "How effective are AI technologies in improving patient outcomes and clinical decision-making in healthcare environments?",
    screened: 4,
    pending: 2,
    owner: {
      name: "Survey Owner",
      email: "owner@example.com",
    },
  },
  "survey-002": {
    ...mockSurveys[1],
    researchQuestion:
      "How do changes in temperature, rainfall, and extreme weather affect crop productivity across regions?",
    screened: 7,
    pending: 5,
    owner: {
      name: "Survey Owner",
      email: "owner@example.com",
    },
  },
  "survey-003": {
    ...mockSurveys[2],
    researchQuestion:
      "What measurable effects does remote work have on employee wellbeing, collaboration, and productivity?",
    screened: 8,
    pending: 0,
    owner: {
      name: "Survey Owner",
      email: "owner@example.com",
    },
  },
};

export const mockArticles: Article[] = [
  {
    id: "1",
    title: "Deep Learning Applications in Medical Image Analysis: A Systematic Review",
    authors: ["Smith, J.", "Johnson, A.", "Williams, B."],
    journal: "Journal of Medical Imaging",
    year: 2024,
    doi: "10.1000/jmi.2024.001",
    status: "INCLUDED",
    abstract:
      "This systematic review examines the current state of deep learning applications in medical image analysis.",
    addedBy: {
      id: "owner",
      name: "Survey Owner",
      role: "Owner",
    },
  },
  {
    id: "2",
    title: "Machine Learning in Healthcare: Opportunities and Challenges",
    authors: ["Chen, L.", "Rodriguez, M."],
    journal: "Nature Medicine",
    year: 2023,
    doi: "10.1038/nm.2023.456",
    status: "PENDING",
    abstract:
      "We present a comprehensive analysis of machine learning applications in healthcare settings.",
    addedBy: {
      id: "reviewer-1",
      name: "Ana Petrova",
      role: "Reviewer",
    },
  },
  {
    id: "3",
    title: "Ethical Considerations in AI-Driven Clinical Decision Support Systems",
    authors: ["Kumar, R.", "Thompson, E.", "Lee, S.", "Davis, K."],
    journal: "The Lancet Digital Health",
    year: 2024,
    doi: "10.1016/s2589-7500(24)00012-3",
    status: "INCLUDED",
    abstract:
      "This paper explores the ethical implications of deploying AI-driven clinical decision support systems.",
    addedBy: {
      id: "reviewer-2",
      name: "Mark Johnson",
      role: "Reviewer",
    },
  },
  {
    id: "4",
    title: "Predictive Analytics for Patient Readmission: A Meta-Analysis",
    authors: ["Garcia, M.", "Anderson, P."],
    journal: "JAMA Network Open",
    year: 2023,
    doi: "10.1001/jamanetworkopen.2023.789",
    status: "EXCLUDED",
    abstract:
      "We conducted a meta-analysis of 85 studies examining the effectiveness of predictive analytics models.",
    addedBy: {
      id: "owner",
      name: "Survey Owner",
      role: "Owner",
    },
  },
  {
    id: "5",
    title:
      "Natural Language Processing in Electronic Health Records: Current State and Future Directions",
    authors: ["Wang, H.", "Brown, T.", "Miller, J."],
    journal: "Journal of Biomedical Informatics",
    year: 2024,
    doi: "10.1016/j.jbi.2024.104321",
    status: "PENDING",
    abstract:
      "This review article synthesizes recent advances in natural language processing techniques.",
    addedBy: {
      id: "reviewer-1",
      name: "Ana Petrova",
      role: "Reviewer",
    },
  },
  {
    id: "6",
    title: "AI-Powered Drug Discovery: Accelerating Pharmaceutical Development",
    authors: ["Patel, N.", "Kim, Y.", "O'Brien, M."],
    journal: "Drug Discovery Today",
    year: 2023,
    doi: "10.1016/j.drudis.2023.103567",
    status: "INCLUDED",
    abstract:
      "We examine how artificial intelligence is revolutionizing the drug discovery pipeline.",
    addedBy: {
      id: "reviewer-2",
      name: "Mark Johnson",
      role: "Reviewer",
    },
  },
];

// export const mockContributors: Contributor[] = [
//   {
//     id: "owner",
//     name: "Survey Owner",
//     email: "owner@example.com",
//     role: "Owner",
//     addedDate: "2024-03-15T09:00:00.000Z",
//   },
//   {
//     id: "reviewer-1",
//     name: "Ana Petrova",
//     email: "ana.petrova@example.com",
//     role: "Reviewer",
//     addedDate: "2024-03-18T13:30:00.000Z",
//   },
//   {
//     id: "reviewer-2",
//     name: "Mark Johnson",
//     email: "mark.johnson@example.com",
//     role: "Reviewer",
//     addedDate: "2024-03-20T10:15:00.000Z",
//   },
// ];
