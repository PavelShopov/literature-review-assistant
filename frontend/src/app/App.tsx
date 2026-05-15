import { useState } from "react";
import { Header } from "./components/Header";
import { ImportSection } from "./components/ImportSection";
import { ArticleCard, type Article, type ArticleStatus } from "./components/ArticleCard";
import { EmptyState } from "./components/EmptyState";
import { ArticleModal } from "./components/ArticleModal";
import { toast } from "sonner";
import { Toaster } from "sonner";
import { RouterProvider } from "react-router";
import { router } from "./routes";

// Mock data for demonstration
const mockArticles: Article[] = [
  {
    id: "1",
    title: "Deep Learning Applications in Medical Image Analysis: A Systematic Review",
    authors: ["Smith, J.", "Johnson, A.", "Williams, B."],
    journal: "Journal of Medical Imaging",
    year: 2024,
    doi: "10.1000/jmi.2024.001",
    status: "INCLUDED",
    abstract: "This systematic review examines the current state of deep learning applications in medical image analysis. We analyzed 150 studies published between 2020-2024, focusing on diagnostic accuracy, clinical implementation, and future directions in AI-assisted medical imaging.",
  },
  {
    id: "2",
    title: "Machine Learning in Healthcare: Opportunities and Challenges",
    authors: ["Chen, L.", "Rodriguez, M."],
    journal: "Nature Medicine",
    year: 2023,
    doi: "10.1038/nm.2023.456",
    status: "PENDING",
    abstract: "We present a comprehensive analysis of machine learning applications in healthcare settings, discussing both the transformative potential and the practical challenges faced during implementation.",
  },
  {
    id: "3",
    title: "Ethical Considerations in AI-Driven Clinical Decision Support Systems",
    authors: ["Kumar, R.", "Thompson, E.", "Lee, S.", "Davis, K."],
    journal: "The Lancet Digital Health",
    year: 2024,
    doi: "10.1016/s2589-7500(24)00012-3",
    status: "INCLUDED",
    abstract: "This paper explores the ethical implications of deploying AI-driven clinical decision support systems in healthcare environments, with focus on bias, transparency, and patient autonomy.",
  },
  {
    id: "4",
    title: "Predictive Analytics for Patient Readmission: A Meta-Analysis",
    authors: ["Garcia, M.", "Anderson, P."],
    journal: "JAMA Network Open",
    year: 2023,
    doi: "10.1001/jamanetworkopen.2023.789",
    status: "EXCLUDED",
    abstract: "We conducted a meta-analysis of 85 studies examining the effectiveness of predictive analytics models in reducing patient readmission rates across various healthcare settings.",
  },
  {
    id: "5",
    title: "Natural Language Processing in Electronic Health Records: Current State and Future Directions",
    authors: ["Wang, H.", "Brown, T.", "Miller, J."],
    journal: "Journal of Biomedical Informatics",
    year: 2024,
    doi: "10.1016/j.jbi.2024.104321",
    status: "PENDING",
    abstract: "This review article synthesizes recent advances in natural language processing techniques applied to electronic health records, highlighting key achievements and identifying areas for future research.",
  },
  {
    id: "6",
    title: "AI-Powered Drug Discovery: Accelerating Pharmaceutical Development",
    authors: ["Patel, N.", "Kim, Y.", "O'Brien, M."],
    journal: "Drug Discovery Today",
    year: 2023,
    doi: "10.1016/j.drudis.2023.103567",
    status: "INCLUDED",
    abstract: "We examine how artificial intelligence is revolutionizing the drug discovery pipeline, from target identification to clinical trial optimization, reducing time and costs in pharmaceutical development.",
  },
];

export default function App() {
  return (
    <>
      <Toaster position="top-right" />
      <RouterProvider router={router} />
    </>
  );
}