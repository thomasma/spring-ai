package com.tryout.ai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service class demonstrating Retrieval-Augmented Generation (RAG) pattern.
 *
 * RAG enhances LLM responses by retrieving relevant context from a knowledge base
 * before generating answers. This approach provides more accurate, fact-based responses
 * grounded in your specific documents.
 *
 * Key concepts:
 * - Document ingestion: Load and chunk documents into manageable pieces
 * - Embeddings: Convert text into vector representations for semantic search
 * - Vector similarity: Find most relevant document chunks for a query
 * - Context augmentation: Include retrieved context in the LLM prompt
 */
@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    // Chunk size for splitting documents (characters)
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;

    @Value("${spring.ai.openai.model:gpt-4o}")
    private String defaultModelName;

    @Value("${spring.ai.openai.temperature:0.7}")
    private double defaultTemperature;

    // In-memory vector store: maps document chunks to their embeddings
    private final List<DocumentChunk> documentStore = new ArrayList<>();

    /**
     * Ingests a PDF document into the RAG system.
     *
     * Process:
     * 1. Extract text from PDF
     * 2. Split into chunks with overlap
     * 3. Generate embeddings for each chunk
     * 4. Store in vector database
     *
     * @param pdfPath path to the PDF file
     * @return number of chunks created
     * @throws IOException if PDF cannot be read
     */
    public int ingestPdfDocument(String pdfPath) throws IOException {
        logger.info("Starting PDF ingestion from: {}", pdfPath);

        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            throw new IOException("PDF file not found: " + pdfPath);
        }

        // Extract text from PDF
        String fullText = extractTextFromPdf(pdfFile);
        logger.info("Extracted {} characters from PDF", fullText.length());

        // Split into chunks
        List<String> chunks = splitIntoChunks(fullText, CHUNK_SIZE, CHUNK_OVERLAP);
        logger.info("Split document into {} chunks", chunks.size());

        // Generate embeddings and store (limit to first 50 chunks for demo)
        int maxChunks = Math.min(chunks.size(), 50);
        logger.info("Processing {} chunks (limited from {} total for demo)", maxChunks, chunks.size());

        int chunksAdded = 0;
        for (int i = 0; i < maxChunks; i++) {
            String chunk = chunks.get(i);
            if (chunk.trim().isEmpty()) {
                continue;
            }

            try {
                // Generate embedding for this chunk
                float[] embedding = embeddingModel.embed(chunk);

                // Create metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", pdfPath);
                metadata.put("chunk_index", i);
                metadata.put("total_chunks", maxChunks);

                // Store chunk with embedding
                DocumentChunk docChunk = new DocumentChunk(chunk, embedding, metadata);
                documentStore.add(docChunk);
                chunksAdded++;

                if (chunksAdded % 10 == 0) {
                    logger.info("Processed {} / {} chunks", chunksAdded, maxChunks);
                }

            } catch (Exception e) {
                logger.warn("Failed to generate embedding for chunk {}: {}", i, e.getMessage());
            }
        }

        logger.info("Successfully ingested {} chunks from {}", chunksAdded, pdfPath);
        return chunksAdded;
    }

    /**
     * Performs RAG-based chat: retrieves relevant context, then generates answer.
     *
     * This is the core RAG pattern:
     * 1. Convert question to embedding
     * 2. Find most similar document chunks
     * 3. Build prompt with retrieved context
     * 4. Generate answer using context
     *
     * @param question user's question
     * @param topK number of most relevant chunks to retrieve (default: 3)
     * @return AI-generated answer based on retrieved context
     */
    public String chatWithDocuments(String question, int topK) {
        logger.info("RAG query: {} (retrieving top {} chunks)", question, topK);

        if (documentStore.isEmpty()) {
            throw new IllegalStateException("No documents have been ingested. Please ingest documents first.");
        }

        // Step 1: Convert question to embedding
        float[] questionEmbedding = embeddingModel.embed(question);

        // Step 2: Find most similar chunks using cosine similarity
        List<DocumentChunk> relevantChunks = findMostSimilar(questionEmbedding, topK);
        logger.info("Retrieved {} relevant chunks", relevantChunks.size());

        // Step 3: Build context from retrieved chunks
        String context = relevantChunks.stream()
                .map(DocumentChunk::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // Step 4: Create RAG prompt
        String ragPrompt = buildRagPrompt(question, context);

        // Step 5: Generate answer
        String response = chatClient.prompt(new Prompt(
                ragPrompt,
                OpenAiChatOptions.builder()
                        .model(defaultModelName)
                        .temperature(defaultTemperature)
                        .build()
        ))
                .call()
                .content();

        logger.info("Generated RAG response (length: {})", response.length());
        return response;
    }

    /**
     * Searches for document chunks most similar to the query.
     * Useful for debugging or showing users what context was retrieved.
     *
     * @param query search query
     * @param topK number of results to return
     * @return list of relevant document chunks with metadata
     */
    public List<Map<String, Object>> searchDocuments(String query, int topK) {
        logger.info("Searching documents for: {} (top {})", query, topK);

        if (documentStore.isEmpty()) {
            logger.warn("Document store is empty");
            return new ArrayList<>();
        }

        float[] queryEmbedding = embeddingModel.embed(query);
        List<DocumentChunk> similarChunks = findMostSimilar(queryEmbedding, topK);

        return similarChunks.stream()
                .map(chunk -> {
                    Map<String, Object> result = new HashMap<>(chunk.getMetadata());
                    result.put("text", chunk.getText());
                    result.put("preview", chunk.getText().substring(0, Math.min(200, chunk.getText().length())) + "...");
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the current size of the document store.
     */
    public int getDocumentCount() {
        return documentStore.size();
    }

    /**
     * Clears all documents from the store.
     */
    public void clearDocuments() {
        documentStore.clear();
        logger.info("Document store cleared");
    }

    // ========== Private Helper Methods ==========

    /**
     * Extracts text content from a PDF file.
     */
    private String extractTextFromPdf(File pdfFile) throws IOException {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Splits text into overlapping chunks.
     * Overlap helps maintain context across chunk boundaries.
     * Memory-efficient implementation for large documents.
     */
    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        int start = 0;

        // For very large documents, process in a streaming fashion
        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            // Try to break at sentence boundary, but limit search range
            if (end < textLength) {
                int searchStart = Math.max(start, end - 200); // Only search last 200 chars
                int lastPeriod = text.lastIndexOf('.', end);
                if (lastPeriod > searchStart) {
                    end = lastPeriod + 1;
                }
            }

            String chunk = text.substring(start, end);
            if (!chunk.trim().isEmpty()) {
                chunks.add(chunk);
            }

            // Move forward, accounting for overlap
            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = start + chunkSize; // Prevent infinite loop
            }
            start = nextStart;
        }

        return chunks;
    }

    /**
     * Finds document chunks most similar to the query embedding using cosine similarity.
     */
    private List<DocumentChunk> findMostSimilar(float[] queryEmbedding, int topK) {
        return documentStore.stream()
                .map(chunk -> {
                    double similarity = cosineSimilarity(queryEmbedding, chunk.getEmbedding());
                    return new ScoredChunk(chunk, similarity);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score)) // Descending order
                .limit(topK)
                .map(scored -> scored.chunk)
                .collect(Collectors.toList());
    }

    /**
     * Calculates cosine similarity between two vectors.
     * Returns value between -1 and 1, where 1 means identical direction.
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Builds the RAG prompt with retrieved context and user question.
     */
    private String buildRagPrompt(String question, String context) {
        return String.format(
            "You are a helpful assistant. Answer the question based on the context provided below. " +
            "If the answer cannot be found in the context, say so.\n\n" +
            "CONTEXT:\n%s\n\n" +
            "QUESTION: %s\n\n" +
            "ANSWER:",
            context,
            question
        );
    }

    // ========== Inner Classes ==========

    /**
     * Represents a document chunk with its embedding and metadata.
     */
    private static class DocumentChunk {
        private final String text;
        private final float[] embedding;
        private final Map<String, Object> metadata;

        public DocumentChunk(String text, float[] embedding, Map<String, Object> metadata) {
            this.text = text;
            this.embedding = embedding;
            this.metadata = metadata;
        }

        public String getText() {
            return text;
        }

        public float[] getEmbedding() {
            return embedding;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * Helper class for sorting chunks by similarity score.
     */
    private static class ScoredChunk {
        private final DocumentChunk chunk;
        private final double score;

        public ScoredChunk(DocumentChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
