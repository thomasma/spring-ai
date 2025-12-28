package com.tryout.ai;

/**
 * Request model for document ingestion into the RAG system.
 *
 * @param filePath absolute or relative path to the document file to ingest
 */
public record DocumentIngestRequest(
    String filePath
) {
}
