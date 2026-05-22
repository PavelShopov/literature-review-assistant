# Architecture Decisions

## [2026-05-22] Dependency Additions for DataService
- **Decision:** Use `org.jbibtex:jbibtex` for parsing BibTeX files.
- **Reasoning:** Lightweight and standard library for robustly handling `.bib` files in Java without much overhead.
- **Decision:** Use `org.apache.pdfbox:pdfbox` for PDF extraction.
- **Reasoning:** Robust, industry-standard library for parsing PDF documents and extracting raw text and metadata. Simple to integrate for our needs.
