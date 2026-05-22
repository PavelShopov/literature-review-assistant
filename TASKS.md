# Project Tasks

## Pending
- [ ] Implement `ArticleServiceImpl` focusing on ingestion methods (`importFromBib`, `uploadPdf`, `importFromUrl`).
- [ ] Implement `ArticleServiceImpl` CRUD operations and relationships to `Survey` and `Author`.
- [ ] Investigate and fix `JAVA_HOME` environment setup if necessary for proper local maven builds.

## In Progress
- None

## Completed
- [x] Create `BibEntry` and `ArticleMetadata` POJOs.
- [x] Implement `BibTexParser` using `jbibtex`.
- [x] Implement `PdfExtractorService` using `pdfbox`.
