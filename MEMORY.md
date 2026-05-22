# Project Memory

## Current Status
Developing a Spring Boot application called "literature-review-assistant" to manage literature reviews, import articles, and run AI annotations. The core `DataService` layer (BibTeX parsing and PDF text extraction) has just been implemented.

## Architecture
- Spring Boot Backend (WebMVC, Data JPA)
- H2 Database (in-memory)
- Thymeleaf for templating
- Data ingestion utilizes `jbibtex` for `.bib` file parsing and `pdfbox` for PDF text extraction.

## Active Components
- `DataService`: Handles parsing raw files (`BibTexParser`, `PdfExtractorService`) into standardized POJOs (`ArticleMetadata`, `BibEntry`).
- `ArticleService`: Interface defining CRUD, import ingestion, survey association, author management, and AI annotation functions for Articles.

## Known Issues
- `ArticleServiceImpl` is not fully implemented yet, so the endpoints or controllers relying on it will not function.
- Local environment: `JAVA_HOME` environment variable was reported as not defined correctly in the terminal during the last test build.

## Recent Changes
- [2026-05-22] Added Lombok to `BibEntry` and `ArticleMetadata`.
- [2026-05-22] Added `jbibtex` and `pdfbox` dependencies to `pom.xml`.
- [2026-05-22] Implemented BibTeX parsing and PDF extraction logic in `DataService`.

## Next Recommended Steps
- Implement `ArticleServiceImpl` to wire up the data ingestion and CRUD methods.
