# Handoff Notes

## Current State
The `DataService` components (`BibTexParser`, `PdfExtractorService`) are fully implemented and integrated with the relevant POJOs. They are capable of parsing uploaded `.bib` files and fetching PDF metadata / text from byte arrays and URLs.

## Next Steps for Future Agents
1. Review `ArticleService.java` to understand the required methods for the primary business logic.
2. Create or update `ArticleServiceImpl` (likely in `service/impl`) to implement the interface.
3. Wire the `DataService` beans (`BibTexParser`, `PdfExtractorService`) inside `ArticleServiceImpl` to handle the data ingestion endpoints.
4. Ensure the database models for `Article`, `Document`, `Author`, and `Survey` are correctly set up and used by the service layer.
